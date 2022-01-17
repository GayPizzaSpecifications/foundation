package cloud.kubelet.foundation.bifrost

import cloud.kubelet.foundation.bifrost.model.BifrostConfig
import cloud.kubelet.foundation.core.FoundationCorePlugin
import cloud.kubelet.foundation.core.Util
import cloud.kubelet.foundation.core.util.AdvancementTitleCache
import com.charleskorn.kaml.Yaml
import io.papermc.paper.event.player.AsyncChatEvent
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Color
import kotlin.io.path.inputStream
import net.dv8tion.jda.api.hooks.EventListener as DiscordEventListener
import org.bukkit.event.Listener as BukkitEventListener

class FoundationBifrostPlugin : JavaPlugin(), DiscordEventListener, BukkitEventListener {
  private lateinit var config: BifrostConfig
  private var jda: JDA? = null
  private var isDev = false

  override fun onEnable() {
    isDev = description.version == "DEV"

    val foundation = server.pluginManager.getPlugin("Foundation") as FoundationCorePlugin
    slF4JLogger.info("Plugin data path: ${foundation.pluginDataPath}")

    val configPath = Util.copyDefaultConfig<FoundationBifrostPlugin>(
      slF4JLogger,
      foundation.pluginDataPath,
      "bifrost.yaml"
    )
    config = Yaml.default.decodeFromStream(BifrostConfig.serializer(), configPath.inputStream())

    server.pluginManager.registerEvents(this, this)
    if (config.authentication.token.isEmpty()) {
      slF4JLogger.warn("Token empty, Bifrost will not connect to Discord.")
      return
    }

    jda = JDABuilder
      .createDefault(config.authentication.token)
      .addEventListeners(this)
      .build()
  }

  override fun onDisable() {
    // Plugin was not initialized, don't do anything.
    if (jda == null) return

    onServerStop()

    logger.info("Shutting down JDA")
    jda?.shutdown()
    while (jda != null && jda!!.status != JDA.Status.SHUTDOWN) {
      Thread.sleep(100)
    }
  }

  override fun onEvent(e: GenericEvent) {
    when (e) {
      is ReadyEvent -> {
        onDiscordReady()
      }
      is MessageReceivedEvent -> {
        if (!config.channel.bridge) return
        // Prevent this bot from receiving its own messages and creating a feedback loop.
        if (e.author.id == jda?.selfUser?.id) return

        // Only forward messages from the configured channel.
        if (e.channel.id != config.channel.id) return

        slF4JLogger.debug(
          "${e.guild.name} - ${e.channel.name} - ${e.author.name}: ${e.message.contentDisplay}"
        )
        server.sendMessage(Component.text("${e.author.name} - ${e.message.contentDisplay}"))
      }
    }
  }

  private fun getTextChannel(): TextChannel? {
    if (jda == null) {
      return null
    }

    val channel = jda?.getTextChannelById(config.channel.id)
    if (channel == null) {
      slF4JLogger.error("Failed to retrieve channel ${config.channel.id}")
    }
    return channel
  }

  private fun message(f: MessageBuilder.() -> Unit) = MessageBuilder().apply(f).build()
  private fun MessageBuilder.embed(f: EmbedBuilder.() -> Unit) {
    setEmbeds(EmbedBuilder().apply(f).build())
  }

  private fun sendChannelMessage(message: Message, debug: () -> String) {
    val channel = getTextChannel()
    channel?.sendMessage(message)?.queue()

    if (config.enableDebugLog) {
      slF4JLogger.info("Send '${debug()}' to Discord")
    }
  }

  private fun sendChannelMessage(message: String): Unit = sendChannelMessage(message {
    setContent(message)
  }) { message }

  private fun sendEmbedMessage(color: Color, message: String): Unit = sendChannelMessage(message {
    embed {
      setAuthor(message)
      setColor(color)
    }
  }) { "[rgb:${color.rgb}] $message" }

  @EventHandler(priority = EventPriority.MONITOR)
  private fun onPlayerJoin(e: PlayerJoinEvent) {
    if (!config.channel.sendPlayerJoin) return

    sendEmbedMessage(Color.GREEN, "${e.player.name} joined the server")
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private fun onPlayerQuit(e: PlayerQuitEvent) {
    if (!config.channel.sendPlayerQuit) return

    sendEmbedMessage(Color.RED, "${e.player.name} left the server")
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private fun onPlayerChat(e: AsyncChatEvent) {
    if (!config.channel.bridge) return
    val message = e.message()

    val messageAsText = LegacyComponentSerializer.legacySection().serialize(message)
    sendChannelMessage("${e.player.name}: $messageAsText")
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private fun onPlayerDeath(e: PlayerDeathEvent) {
    if (!config.channel.sendPlayerDeath) return
    @Suppress("DEPRECATION")
    var deathMessage = e.deathMessage
    if (deathMessage == null || deathMessage.isBlank()) {
      deathMessage = "${e.player.name} died"
    }
    sendEmbedMessage(Color.YELLOW, deathMessage)
  }

  @EventHandler(priority = EventPriority.MONITOR)
  private fun onPlayerAdvancementDone(e: PlayerAdvancementDoneEvent) {
    if (!config.channel.sendPlayerAdvancement) return
    if (e.advancement.key.key.contains("recipe/")) {
      return
    }

    val display = AdvancementTitleCache.of(e.advancement) ?: return
    sendEmbedMessage(Color.CYAN, "${e.player.name} completed the advancement '${display}'")
  }

  private fun onDiscordReady() {
    if (!config.channel.sendStart) return
    if (isDev) return
    sendChannelMessage(":white_check_mark: Server is ready!")
  }

  private fun onServerStop() {
    if (!config.channel.sendShutdown) return
    if (isDev) return
    sendChannelMessage(":octagonal_sign: Server is stopping!")
  }
}
