package gay.pizza.foundation.common

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.plugin.java.JavaPlugin

abstract class BaseFoundationPlugin : JavaPlugin() {
  fun registerCommandExecutor(name: String, executor: CommandExecutor) {
    registerCommandExecutor(listOf(name), executor)
  }

  fun registerCommandExecutor(names: List<String>, executor: CommandExecutor) {
    for (name in names) {
      val command = getCommand(name) ?: throw Exception("Failed to get $name command")
      command.setExecutor(executor)
      if (executor is TabCompleter) {
        command.tabCompleter = executor
      }
    }
  }
}
