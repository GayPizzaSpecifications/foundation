package cloud.kubelet.foundation.gjallarhorn.util

fun <T> Sequence<T>.minOfAll(fieldCount: Int, block: (value: T) -> List<Long>): List<Long> {
  val fieldRange = 0 until fieldCount
  val results = fieldRange.map { Long.MAX_VALUE }.toMutableList()
  for (item in this) {
    val numerics = block(item)
    for (field in fieldRange) {
      val current = results[field]
      val number = numerics[field]
      if (number < current) {
        results[field] = number
      }
    }
  }
  return results
}

fun <T> Sequence<T>.maxOfAll(fieldCount: Int, block: (value: T) -> List<Long>): List<Long> {
  val fieldRange = 0 until fieldCount
  val results = fieldRange.map { Long.MIN_VALUE }.toMutableList()
  for (item in this) {
    val numerics = block(item)
    for (field in fieldRange) {
      val current = results[field]
      val number = numerics[field]
      if (number > current) {
        results[field] = number
      }
    }
  }
  return results
}
