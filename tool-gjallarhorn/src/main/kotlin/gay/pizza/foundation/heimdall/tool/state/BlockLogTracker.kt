package gay.pizza.foundation.heimdall.tool.state

import gay.pizza.foundation.heimdall.tool.util.maxOfAll
import gay.pizza.foundation.heimdall.tool.util.minOfAll
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

class BlockLogTracker(isConcurrent: Boolean = false) {
  internal val blocks: MutableMap<BlockCoordinate, BlockState> = if (isConcurrent) ConcurrentHashMap() else mutableMapOf()

  fun place(position: BlockCoordinate, state: BlockState) {
    blocks[position] = state
  }

  fun placeAll(map: Map<BlockCoordinate, BlockState>) {
    blocks.putAll(map)
  }

  fun calculateZeroBlockOffset(): BlockCoordinate {
    val (x, y, z) = blocks.keys.minOfAll(3) { listOf(it.x, it.y, it.z) }
    val xOffset = if (x < 0) x.absoluteValue else 0
    val yOffset = if (y < 0) y.absoluteValue else 0
    val zOffset = if (z < 0) z.absoluteValue else 0

    return BlockCoordinate(xOffset, yOffset, zOffset)
  }

  fun calculateMaxBlock(): BlockCoordinate {
    val (x, y, z) = blocks.keys.maxOfAll(3) { listOf(it.x, it.y, it.z) }
    return BlockCoordinate(x, y, z)
  }

  fun isEmpty() = blocks.isEmpty()
  fun isNotEmpty() = !isEmpty()

  fun buildBlockMap(offset: BlockCoordinate = BlockCoordinate.zero): SparseBlockStateMap {
    val map = SparseBlockStateMap()
    blocks.forEach { (position, state) ->
      val realPosition = offset.applyAsOffset(position)
      map.put(realPosition, state)
    }
    return map
  }

  fun <T> buildBlockMap(offset: BlockCoordinate = BlockCoordinate.zero, value: (BlockState) -> T): BlockCoordinateSparseMap<T> {
    val map = BlockCoordinateSparseMap<T>()
    blocks.forEach { (position, state) ->
      val realPosition = offset.applyAsOffset(position)
      map.put(realPosition, value(state))
    }
    return map
  }

  fun replay(changelog: BlockChangelog) = changelog.changes.forEach { change ->
    place(change.location, change.state)
  }

  fun get(position: BlockCoordinate): BlockState? = blocks[position]
}
