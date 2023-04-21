package gay.pizza.foundation.core.features.player

import gay.pizza.foundation.common.spawn
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed

class MegaTntCommand : CommandExecutor {
  override fun onCommand(
    sender: CommandSender,
    command: Command,
    label: String,
    args: Array<out String>): Boolean {
    if (sender !is Player) {
      sender.sendMessage("Player is required for this command.")
      return true
    }
    val tnt = sender.spawn(TNTPrimed::class)
    tnt.fuseTicks = 1
    tnt.yield = 50.0f
    return true
  }
}
