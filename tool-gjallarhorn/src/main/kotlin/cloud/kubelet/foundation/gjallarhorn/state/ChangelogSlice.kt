package cloud.kubelet.foundation.gjallarhorn.state

import java.time.Duration
import java.time.Instant

data class ChangelogSlice(val from: Instant, val to: Instant, val relative: Duration) {
  constructor(from: Instant, to: Instant) : this(from, to, Duration.ofMillis(to.toEpochMilli() - from.toEpochMilli()))

  val relativeChangeStart: Instant = to.minus(relative)
  val range: ClosedRange<Instant> = from..to
  val relativeChangeRange: ClosedRange<Instant> = relativeChangeStart..to

  fun isTimeWithin(time: Instant) = time in range
  fun isRelativeWithin(time: Instant) = time in relativeChangeRange

  fun split(): List<ChangelogSlice> {
    val half = relative.dividedBy(2)
    val initial = to.minus(relative)
    val first = initial.plus(half)
    return listOf(
      ChangelogSlice(from, first, half),
      ChangelogSlice(from, to, half)
    )
  }
}
