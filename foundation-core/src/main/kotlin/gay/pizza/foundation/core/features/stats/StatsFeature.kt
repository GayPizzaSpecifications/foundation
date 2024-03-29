package gay.pizza.foundation.core.features.stats

import gay.pizza.foundation.core.abstraction.Feature
import gay.pizza.foundation.core.features.persist.PersistentStoreCommand
import gay.pizza.foundation.core.features.persist.setAllProperties
import gay.pizza.foundation.shared.PersistentStore
import gay.pizza.foundation.shared.PluginPersistence
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TextComponent
import org.bukkit.event.EventHandler
import org.koin.core.component.inject
import java.time.Instant

@Suppress("IdentifierGrammar")
class StatsFeature : Feature() {
  internal val persistence by inject<PluginPersistence>()
  private lateinit var chatLogStore: PersistentStore

  override fun enable() {
    chatLogStore = persistence.store("chat-logs")

    plugin.registerCommandExecutor(listOf("leaderboard", "lb"), LeaderboardCommand())
    plugin.registerCommandExecutor("pstore", PersistentStoreCommand(this))
  }

  @EventHandler
  private fun logOnChatMessage(e: AsyncChatEvent) {
    val player = e.player
    val message = e.message()

    if (message !is TextComponent) {
      return
    }

    val content = message.content()
    chatLogStore.create("ChatMessageEvent") {
      setAllProperties(
        "timestamp" to Instant.now().toEpochMilli(),
        "player.id" to player.identity().uuid().toString(),
        "player.name" to player.name,
        "message.content" to content
      )
    }
  }
}
