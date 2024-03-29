package gay.pizza.foundation.heimdall.plugin.event

import gay.pizza.foundation.heimdall.plugin.buffer.EventBuffer
import gay.pizza.foundation.heimdall.plugin.buffer.IEventBuffer
import gay.pizza.foundation.heimdall.plugin.model.HeimdallConfig
import gay.pizza.foundation.heimdall.table.PlayerDeathTable
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import java.time.Instant
import java.util.*

class PlayerDeath(
  val playerUniqueIdentity: UUID,
  val location: Location,
  val experienceLevel: Float,
  val deathMessage: String?,
  val timestamp: Instant = Instant.now()
) : HeimdallEvent() {
  constructor(event: PlayerDeathEvent, deathMessage: String? = null) : this(
    event.player.uniqueId,
    event.player.location.clone(),
    event.player.exp,
    deathMessage
  )

  override fun store(transaction: Transaction, index: Int) {
    transaction.apply {
      PlayerDeathTable.insert {
        putPlayerTimedLocalEvent(it, timestamp, location, playerUniqueIdentity)
        it[experience] = experienceLevel.toDouble()
        it[message] = deathMessage
      }
    }
  }

  class Collector(val buffer: IEventBuffer) : EventCollector<PlayerDeath> {
    private val legacyComponentSerializer = LegacyComponentSerializer.builder().build()

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
      val deathMessage = event.deathMessage()
      val deathMessageString = if (deathMessage != null) {
        legacyComponentSerializer.serialize(deathMessage)
      } else {
        null
      }
      buffer.push(PlayerDeath(event, deathMessageString))
    }
  }

  companion object : EventCollectorProvider<PlayerDeath> {
    override fun collector(config: HeimdallConfig, buffer: EventBuffer): EventCollector<PlayerDeath> = Collector(buffer)
  }
}
