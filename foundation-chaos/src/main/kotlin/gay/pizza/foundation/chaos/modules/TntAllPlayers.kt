package gay.pizza.foundation.chaos.modules

import gay.pizza.foundation.common.spawn
import org.bukkit.entity.TNTPrimed
import org.bukkit.plugin.Plugin

class TntAllPlayers(val plugin: Plugin) : ChaosModule {
  override fun id(): String = "tnt-all-players"
  override fun name(): String = "TNT Us All"
  override fun what(): String = "TNT All Players"

  override fun activate() {
    for (player in plugin.server.onlinePlayers) {
      player.spawn(TNTPrimed::class)
    }
  }
}
