package cloud.kubelet.foundation.core.features.backup

import cloud.kubelet.foundation.core.FoundationCorePlugin
import cloud.kubelet.foundation.core.Util
import cloud.kubelet.foundation.core.abstraction.Feature
import cloud.kubelet.foundation.core.features.scheduler.cancel
import cloud.kubelet.foundation.core.features.scheduler.cron
import com.charleskorn.kaml.Yaml
import org.koin.core.component.inject
import org.koin.dsl.module
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI
import kotlin.io.path.inputStream

class BackupFeature : Feature() {
  private val plugin by inject<FoundationCorePlugin>()
  private val s3Client by inject<S3Client>()
  private val config by inject<BackupConfig>()
  private lateinit var scheduleId: String

  override fun enable() {
    // Create backup directory.
    val backupPath = plugin.pluginDataPath.resolve(BACKUPS_DIRECTORY)
    backupPath.toFile().mkdir()

    registerCommandExecutor("fbackup", BackupCommand(plugin, backupPath, config, s3Client))

    if (config.schedule.cron.isNotEmpty()) {
      // Assume user never wants to modify second. I'm not sure why this is enforced in Quartz.
      val expr = "0 ${config.schedule.cron}"
      scheduleId = scheduler.cron(expr) {
        plugin.server.scheduler.runTask(plugin) { ->
          plugin.server.dispatchCommand(plugin.server.consoleSender, "fbackup")
        }
      }
    }
  }

  override fun disable() {
    if (::scheduleId.isInitialized) {
      scheduler.cancel(scheduleId)
    }
  }

  override fun module() = module {
    single {
      val configPath = Util.copyDefaultConfig<FoundationCorePlugin>(
        plugin.slF4JLogger,
        plugin.pluginDataPath,
        "backup.yaml",
      )
      return@single Yaml.default.decodeFromStream(
        BackupConfig.serializer(),
        configPath.inputStream()
      )
    }
    single {
      val config = get<BackupConfig>()

      val creds = StaticCredentialsProvider.create(
        AwsSessionCredentials.create(config.s3.accessKeyId, config.s3.secretAccessKey, "")
      )
      val builder = S3Client.builder().credentialsProvider(creds)

      if (config.s3.endpointOverride.isNotEmpty()) {
        builder.endpointOverride(URI.create(config.s3.endpointOverride))
      }

      if (config.s3.region.isNotEmpty()) {
        builder.region(Region.of(config.s3.region))
      } else {
        builder.region(Region.US_WEST_1)
      }

      builder.build()
    }
  }

  companion object {
    private const val BACKUPS_DIRECTORY = "backups"
  }
}