package gay.pizza.foundation.chaos.modules

import org.bukkit.plugin.Plugin

object ChaosModules {
  fun all(plugin: Plugin) = listOf(
    NearestPlayerEntitySpawn(plugin),
    TeleportAllEntitiesNearestPlayer(plugin)
  )
}
