package gay.pizza.foundation.core.features.update

import gay.pizza.foundation.concrete.ExtensibleManifest
import kotlinx.serialization.json.Json
import org.bukkit.Server
import java.net.URI

@Suppress("UnstableApiUsage")
class UpdateResolver {
  fun fetchCurrentManifest(): ExtensibleManifest {
    val jsonContentString = latestManifestUrl.openStream().readAllBytes().decodeToString()
    return jsonRelaxed.decodeFromString(ExtensibleManifest.serializer(), jsonContentString)
  }

  fun resolve(manifest: ExtensibleManifest, server: Server): UpdatePlan {
    val installedPlugins = server.pluginManager.plugins.associateBy {
      val key = it.name.lowercase()
      val nameOverride = pluginToManifestNameMappings[key]
      nameOverride ?: key
    }
    val installSet = manifest.items
      .filter { it.type == "bukkit-plugin" }
      .filter { installedPlugins.containsKey(it.name) }
      .associateWith { installedPlugins[it.name] }
      .toMutableMap()

    var lastCount = 0
    while (lastCount < installSet.size) {
      lastCount = installSet.size
      val installSetNames = installSet.keys.map { it.name }
      val totalDependencySet = installSet.keys.flatMap { it.dependencies }.toSet()
      for (dependencyName in totalDependencySet.filter { !installSetNames.contains(it) }) {
        val newDependency = installSet.keys.firstOrNull { it.name == dependencyName } ?:
          throw RuntimeException("Unresolved Dependency: $dependencyName")
        installSet[newDependency] = null
      }
    }

    val updateSet = installSet.filter { entry ->
      if (entry.value == null) {
        true
      } else {
        val installed = entry.value!!.pluginMeta.version
        if (installed == "DEV") {
          false
        } else {
          entry.key.version != installed
        }
      }
    }
    return UpdatePlan(installSet, updateSet)
  }

  companion object {
    internal val latestManifestUrl = URI("https://artifacts.gay.pizza/foundation/manifest.json").toURL()
    private val jsonRelaxed = Json { ignoreUnknownKeys = true }

    private val pluginToManifestNameMappings = mapOf(
      "foundation" to "foundation-core"
    )
  }
}
