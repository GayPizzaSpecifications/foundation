package gay.pizza.foundation.shared

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.serialization.DeserializationStrategy
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.io.path.inputStream

/**
 * Copy the default configuration from the resource [resourceName] into the directory [targetPath].
 * @param targetPath The output directory as a path, it must exist before calling this.
 * @param resourceName Path to resource, it should be in the root of the `resources` directory,
 *  without the leading slash.
 */
inline fun <reified T> copyDefaultConfig(log: Logger, targetPath: Path, resourceName: String): Path {
  if (resourceName.startsWith("/")) {
    throw IllegalArgumentException("resourceName starts with slash")
  }

  if (!targetPath.toFile().exists()) {
    throw Exception("Configuration output path does not exist!")
  }
  val outPath = targetPath.resolve(resourceName)
  val outFile = outPath.toFile()
  if (outFile.exists()) {
    return outPath
  }

  val resourceStream = T::class.java.getResourceAsStream("/$resourceName")
    ?: throw Exception("Configuration resource does not exist!")
  val outputStream = outFile.outputStream()

  resourceStream.use {
    outputStream.use {
      log.info("Copied default configuration to $outPath")
      resourceStream.copyTo(outputStream)
    }
  }

  return outPath
}

inline fun <reified T> loadConfigurationWithDefault(
  log: Logger,
  deserializer: DeserializationStrategy<T>,
  pluginConfigDirectoryPath: Path,
  name: String
): T {
  val path = copyDefaultConfig<T>(log, pluginConfigDirectoryPath, name)
  return Yaml.default.decodeFromStream(deserializer, path.inputStream())
}
