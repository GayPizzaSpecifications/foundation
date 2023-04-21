package gay.pizza.foundation.core.features.player

import gay.pizza.foundation.common.chat
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GooseCommand : CommandExecutor {
  override fun onCommand(
    sender: CommandSender,
    command: Command,
    label: String,
    args: Array<out String>): Boolean {
    if (sender !is Player) {
      sender.sendMessage("Player is required for this command.")
      return true
    }
    sender.chat(
      "Goose is the most beautiful kitty to ever exist <3",
      "I don't know who Nat is but there is no way she can compare to Goose."
    )
    return true
  }
}
