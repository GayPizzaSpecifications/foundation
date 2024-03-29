package gay.pizza.foundation.chaos.modules

import gay.pizza.foundation.chaos.nearestPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.plugin.Plugin

class NearestPlayerEntitySpawn(val plugin: Plugin) : ChaosModule {
  override fun id(): String = "nearest-player-entity-spawn"
  override fun name(): String = "Monster Me"
  override fun what(): String = "Teleport all spawned entities to the nearest player"

  @EventHandler
  fun onMobSpawn(e: EntitySpawnEvent) {
    val player = e.location.nearestPlayer()
    if (player != null) {
      e.entity.server.scheduler.runTask(plugin) { ->
        e.entity.teleport(player)
      }
    }
  }
}
