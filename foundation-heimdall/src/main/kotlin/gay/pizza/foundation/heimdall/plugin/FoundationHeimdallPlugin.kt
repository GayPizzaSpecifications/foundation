package gay.pizza.foundation.heimdall.plugin

import com.charleskorn.kaml.Yaml
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gay.pizza.foundation.common.FoundationCoreLoader
import gay.pizza.foundation.shared.PluginMainClass
import gay.pizza.foundation.shared.copyDefaultConfig
import gay.pizza.foundation.heimdall.plugin.buffer.BufferFlushThread
import gay.pizza.foundation.heimdall.plugin.buffer.EventBuffer
import gay.pizza.foundation.heimdall.plugin.event.*
import gay.pizza.foundation.heimdall.plugin.export.ExportAllChunksCommand
import gay.pizza.foundation.heimdall.plugin.load.ImportWorldLoadCommand
import gay.pizza.foundation.heimdall.plugin.model.HeimdallConfig
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.postgresql.Driver
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.inputStream

@PluginMainClass
class FoundationHeimdallPlugin : JavaPlugin(), Listener {
  private lateinit var config: HeimdallConfig
  private lateinit var pool: HikariDataSource
  internal var db: Database? = null

  private val buffer = EventBuffer()
  private val bufferFlushThread = BufferFlushThread(this, buffer)

  private val playerJoinTimes = ConcurrentHashMap<UUID, Instant>()

  private val legacyComponentSerializer = LegacyComponentSerializer.builder().build()

  override fun onEnable() {
    val exportChunksCommand = getCommand("export_all_chunks") ?: throw Exception("Failed to get export_all_chunks command")
    exportChunksCommand.setExecutor(ExportAllChunksCommand(this))

    val importWorldLoadCommand = getCommand("import_world_load") ?: throw Exception("Failed to get import_world_load command")
    importWorldLoadCommand.setExecutor(ImportWorldLoadCommand(this))

    val foundation = FoundationCoreLoader.get(server)
    val configPath = copyDefaultConfig<FoundationHeimdallPlugin>(
      slF4JLogger,
      foundation.pluginDataPath,
      "heimdall.yaml"
    )
    config = Yaml.default.decodeFromStream(HeimdallConfig.serializer(), configPath.inputStream())
    if (!config.enabled) {
      slF4JLogger.info("Heimdall is not enabled.")
      return
    }
    slF4JLogger.info("Heimdall is enabled.")
    if (!Driver.isRegistered()) {
      Driver.register()
    }
    pool = HikariDataSource(HikariConfig().apply {
      jdbcUrl = config.db.url
      username = config.db.username
      password = config.db.password
      maximumPoolSize = 10
      idleTimeout = Duration.ofMinutes(5).toMillis()
      maxLifetime = Duration.ofMinutes(10).toMillis()
    })
    val initMigrationContent = FoundationHeimdallPlugin::class.java.getResourceAsStream(
      "/init.sql"
    )?.readAllBytes()?.decodeToString() ?: throw RuntimeException("Unable to find Heimdall init.sql")

    val statements = sqlSplitStatements(initMigrationContent)

    pool.connection.use { conn ->
      conn.autoCommit = false
      try {
        for (statementAsString in statements) {
          conn.prepareStatement(statementAsString).use {
            it.execute()
          }
        }
        conn.commit()
      } catch (e: Exception) {
        conn.rollback()
        throw e
      } finally {
        conn.autoCommit = true
      }
    }

    db = Database.connect(pool)
    server.pluginManager.registerEvents(this, this)
    bufferFlushThread.start()
  }

  @EventHandler
  fun onPlayerMove(event: PlayerMoveEvent) = buffer.push(PlayerPosition(event))

  @EventHandler
  fun onBlockBroken(event: BlockPlaceEvent) = buffer.push(BlockPlace(event))

  @EventHandler
  fun onBlockBroken(event: BlockBreakEvent) = buffer.push(BlockBreak(event))

  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    playerJoinTimes[event.player.uniqueId] = Instant.now()
  }

  @EventHandler
  fun onPlayerQuit(event: PlayerQuitEvent) {
    val startTime = playerJoinTimes.remove(event.player.uniqueId) ?: return
    val endTime = Instant.now()
    buffer.push(PlayerSession(event.player.uniqueId, event.player.name, startTime, endTime))
  }

  @EventHandler
  fun onPlayerDeath(event: PlayerDeathEvent) {
    val deathMessage = event.deathMessage()
    val deathMessageString = if (deathMessage != null) {
      legacyComponentSerializer.serialize(deathMessage)
    } else {
      null
    }
    buffer.push(PlayerDeath(event, deathMessageString))
  }

  @EventHandler
  fun onPlayerAdvancementDone(event: PlayerAdvancementDoneEvent) = buffer.push(PlayerAdvancement(event))

  @EventHandler
  fun onWorldLoad(event: PlayerChangedWorldEvent) = buffer.push(
    WorldChange(
      event.player.uniqueId,
      event.from.uid,
      event.from.name,
      event.player.world.uid,
      event.player.world.name
    )
  )

  @EventHandler
  fun onEntityDeath(event: EntityDeathEvent) {
    val killer = event.entity.killer ?: return
    buffer.push(
      EntityKill(
        killer.uniqueId,
        killer.location,
        event.entity.uniqueId,
        event.entityType.key.toString()
      )
    )
  }

  override fun onDisable() {
    bufferFlushThread.stop()
    val endTime = Instant.now()
    for (playerId in playerJoinTimes.keys().toList()) {
      val startTime = playerJoinTimes.remove(playerId) ?: continue
      buffer.push(PlayerSession(
        playerId,
        server.getPlayer(playerId)?.name ?: "__unknown__",
        startTime,
        endTime
      ))
    }
    bufferFlushThread.flush()
  }
}
