package gay.pizza.foundation.chaos.modules

import gay.pizza.foundation.common.spawn
import org.bukkit.entity.TNTPrimed
import org.bukkit.plugin.Plugin

class MegaTnt(val plugin: Plugin) : ChaosModule {
  override fun id(): String = "mega-tnt"
  override fun name(): String = "Mega TNT"
  override fun what(): String = "Spawn a massive TNT explosion"

  override fun activate() {
    for (player in plugin.server.onlinePlayers) {
      val tnt = player.spawn(TNTPrimed::class)
      tnt.fuseTicks = 1
      tnt.yield = 10.0f
    }
  }
}
