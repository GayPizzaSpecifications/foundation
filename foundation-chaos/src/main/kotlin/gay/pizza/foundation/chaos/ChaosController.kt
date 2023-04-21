package gay.pizza.foundation.chaos

import gay.pizza.foundation.chaos.model.ChaosConfig
import gay.pizza.foundation.chaos.modules.ChaosModule
import gay.pizza.foundation.chaos.modules.ChaosModules
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.util.concurrent.atomic.AtomicBoolean

class ChaosController(val plugin: Plugin, val config: ChaosConfig) : Listener {
  val state: AtomicBoolean = AtomicBoolean(false)
  val selectorController = ChaosSelectorController(this, plugin)

  val allModules = ChaosModules.all(plugin)
  var allowedModules: List<ChaosModule> = emptyList()

  private var activeModules = mutableSetOf<ChaosModule>()

  var bossBar: BossBar? = null

  fun load() {
    if (state.get()) {
      return
    }
    allowedModules = filterEnabledModules()
    state.set(true)
    selectorController.schedule()
    bossBar = plugin.server.createBossBar("Chaos Mode", BarColor.RED, BarStyle.SOLID)
    for (player in plugin.server.onlinePlayers) {
      bossBar?.addPlayer(player)
    }
  }

  private fun filterEnabledModules(): List<ChaosModule> = allModules.filter { module ->
    val moduleConfig = config.modules[module.id()] ?: config.defaultModuleConfiguration
    moduleConfig.enabled
  }

  fun activateAll() {
    for (module in allowedModules) {
      if (activeModules.contains(module)) {
        continue
      }
      activate(module)
    }
  }

  fun activate(module: ChaosModule) {
    plugin.server.pluginManager.registerEvents(module, plugin)
    module.activate()
    activeModules.add(module)
    updateBossBar()
  }

  fun deactivate(module: ChaosModule) {
    HandlerList.unregisterAll(module)
    module.deactivate()
    activeModules.remove(module)
    updateBossBar()
  }

  fun updateBossBar() {
    val activeModuleText = activeModules.joinToString(", ") { it.name() }
    bossBar?.setTitle("Chaos Mode: $activeModuleText")
  }

  fun deactivateAll() {
    for (module in activeModules.toList()) {
      deactivate(module)
    }
  }

  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    bossBar?.addPlayer(event.player)
  }

  fun unload() {
    if (!state.get()) {
      return
    }
    deactivateAll()
    bossBar?.removeAll()
    bossBar = null
    state.set(false)
    selectorController.cancel()
  }
}
