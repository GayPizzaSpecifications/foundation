package cloud.kubelet.foundation.core.persist

import jetbrains.exodus.entitystore.Entity

fun <T : Comparable<*>> Entity.setAllProperties(vararg entries: Pair<String, T>) = entries.forEach { entry ->
  setProperty(entry.first, entry.second)
}