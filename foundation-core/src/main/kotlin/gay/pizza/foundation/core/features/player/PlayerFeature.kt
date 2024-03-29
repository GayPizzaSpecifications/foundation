package gay.pizza.foundation.core.features.player

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalCause
import gay.pizza.foundation.core.abstraction.Feature
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.inject
import java.time.Duration

class PlayerFeature : Feature() {
  private val config by inject<PlayerConfig>()
  private lateinit var playerActivity: Cache<String, String>

  override fun enable() {
    playerActivity = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(config.antiIdle.idleDuration.toLong()))
      .removalListener<String, String> z@{
        if (!config.antiIdle.enabled) return@z
        if (it.cause == RemovalCause.EXPIRED) {
          if (!config.antiIdle.ignore.contains(it.key!!)) {
            plugin.server.scheduler.runTask(plugin) { ->
              plugin.server.getPlayer(it.key!!)
                ?.kick(Component.text("Kicked for idling"), PlayerKickEvent.Cause.IDLING)
            }
          }
        }
      }.build()

    // Expire player activity tokens occasionally.
    plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, {
      playerActivity.cleanUp()
    }, 20, 100)

    plugin.registerCommandExecutor(listOf("survival", "s"), GamemodeCommand(GameMode.SURVIVAL))
    plugin.registerCommandExecutor(listOf("creative", "c"), GamemodeCommand(GameMode.CREATIVE))
    plugin.registerCommandExecutor(listOf("adventure", "a"), GamemodeCommand(GameMode.ADVENTURE))
    plugin.registerCommandExecutor(listOf("spectator", "sp"), GamemodeCommand(GameMode.SPECTATOR))
    plugin.registerCommandExecutor(listOf("localweather", "lw"), LocalWeatherCommand())
    plugin.registerCommandExecutor(listOf("goose", "the_most_wonderful_kitty_ever"), GooseCommand())
    plugin.registerCommandExecutor(listOf("megatnt"), MegaTntCommand())
  }

  override fun module() = org.koin.dsl.module {
    single {
      plugin.loadConfigurationWithDefault(
        plugin,
        PlayerConfig.serializer(),
        "player.yaml"
      )
    }
  }

  @EventHandler
  private fun onPlayerJoin(e: PlayerJoinEvent) {
    if (!config.antiIdle.enabled) return

    playerActivity.put(e.player.name, e.player.name)
  }

  @EventHandler
  private fun onPlayerQuit(e: PlayerQuitEvent) {
    if (!config.antiIdle.enabled) return

    playerActivity.invalidate(e.player.name)
  }

  @EventHandler
  private fun onPlayerMove(e: PlayerMoveEvent) {
    if (!config.antiIdle.enabled) return

    if (e.hasChangedPosition() || e.hasChangedOrientation()) {
      playerActivity.put(e.player.name, e.player.name)
    }
  }
}
