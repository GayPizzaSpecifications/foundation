package gay.pizza.foundation.heimdall.plugin

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import gay.pizza.foundation.common.BaseFoundationPlugin
import gay.pizza.foundation.common.FoundationCoreLoader
import gay.pizza.foundation.heimdall.plugin.buffer.BufferFlushThread
import gay.pizza.foundation.heimdall.plugin.buffer.EventBuffer
import gay.pizza.foundation.heimdall.plugin.event.EventCollector
import gay.pizza.foundation.heimdall.plugin.event.EventCollectorProviders
import gay.pizza.foundation.heimdall.plugin.export.ExportAllChunksCommand
import gay.pizza.foundation.heimdall.plugin.load.ImportWorldLoadCommand
import gay.pizza.foundation.heimdall.plugin.model.HeimdallConfig
import gay.pizza.foundation.shared.PluginMainClass
import org.bukkit.event.Listener
import org.jetbrains.exposed.sql.Database
import org.postgresql.Driver
import java.time.Duration

@PluginMainClass
class FoundationHeimdallPlugin : BaseFoundationPlugin(), Listener {
  private lateinit var config: HeimdallConfig
  private lateinit var pool: HikariDataSource
  internal var db: Database? = null

  private val buffer = EventBuffer()
  private val bufferFlushThread = BufferFlushThread(this, buffer)

  val collectors = mutableListOf<EventCollector<*>>()

  override fun onEnable() {
    val exportChunksCommand = getCommand("export_all_chunks") ?:
      throw Exception("Failed to get export_all_chunks command")
    exportChunksCommand.setExecutor(ExportAllChunksCommand(this))

    registerCommandExecutor("export_all_chunks", ExportAllChunksCommand(this))
    registerCommandExecutor("export_world_load", ExportAllChunksCommand(this))
    registerCommandExecutor("import_world_load", ExportAllChunksCommand(this))

    val importWorldLoadCommand = getCommand("import_world_load") ?:
      throw Exception("Failed to get import_world_load command")
    importWorldLoadCommand.setExecutor(ImportWorldLoadCommand(this))

    val foundation = FoundationCoreLoader.get(server)
    config = loadConfigurationWithDefault(
      foundation,
      HeimdallConfig.serializer(),
      "heimdall.yaml"
    )
    if (!config.enabled) {
      slF4JLogger.info("Heimdall tracking is not enabled.")
      return
    }
    slF4JLogger.info("Heimdall tracking is enabled.")
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

    for (collectorProvider in EventCollectorProviders.all) {
      val collector = collectorProvider.collector(config, buffer)
      server.pluginManager.registerEvents(collector, this)
      collectors.add(collector)
    }
  }

  override fun onDisable() {
    bufferFlushThread.stop()
    for (collector in collectors) {
      collector.onPluginDisable(server)
    }
    collectors.clear()
    bufferFlushThread.flush()
  }
}
