package gay.pizza.foundation.shared

import java.util.concurrent.ConcurrentHashMap

class PluginPersistence(val core: IFoundationCore) {
  val stores = ConcurrentHashMap<String, PersistentStore>()

  /**
   * Fetch a persistent store by name. Make sure the name is path-safe, descriptive and consistent across server runs.
   */
  fun store(name: String): PersistentStore =
    stores.getOrPut(name) { PersistentStore(core, name) }

  fun unload() {
    stores.values.forEach { store -> store.close() }
    stores.clear()
  }
}
