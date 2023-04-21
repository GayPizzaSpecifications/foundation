package gay.pizza.foundation.common

import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.Statistic
import org.bukkit.entity.EntityType

val Server.allPlayers: List<OfflinePlayer>
  get() = listOf(onlinePlayers, offlinePlayers.filter { !isPlayerOnline(it) }.toList()).flatten()

fun Server.isPlayerOnline(player: OfflinePlayer): Boolean =
  onlinePlayers.any { onlinePlayer -> onlinePlayer.name == player.name }

fun Server.allPlayerStatisticsOf(
  statistic: Statistic,
  material: Material? = null,
  entityType: EntityType? = null,
  order: SortOrder = SortOrder.Ascending
): List<Pair<OfflinePlayer, Int>> = allPlayers.map { player ->
  player to if (material != null) {
    player.getStatistic(statistic, material)
  } else if (entityType != null) {
    player.getStatistic(statistic, entityType)
  } else {
    player.getStatistic(statistic)
  }
}.sortedBy(order) { it.second }
