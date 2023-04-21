package gay.pizza.foundation.chaos.modules

import gay.pizza.foundation.chaos.randomPlayer
import gay.pizza.foundation.common.without
import org.bukkit.Chunk
import org.bukkit.ChunkSnapshot
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

class WorldSwapper(val plugin: Plugin) : ChaosModule {
  override fun id(): String = "world-swapper"
  override fun name(): String = "World Swapper"
  override fun what(): String = "Swaps the world vertically on activation, and un-swaps it on deactivation."

  var chunkInversions = mutableListOf<ChunkInversion>()

  override fun activate() {
    val player = plugin.server.randomPlayer() ?: return
    val baseChunk = player.chunk
    recordInvert(baseChunk)
    player.teleport(player.location.toHighestLocation())
    val chunksToInvert = player.world.loadedChunks.without(baseChunk).toMutableList()

    println("Inverting ${chunksToInvert.size} chunks...")
    plugin.server.scheduler.scheduleUntilEmpty(plugin, chunksToInvert, 5) { chunk ->
      recordInvert(chunk)
    }
  }

  fun recordInvert(chunk: Chunk) {
    chunkInversions.add(invertChunk(chunk))
  }

  override fun deactivate() {
    fun scheduleOne() {
      if (chunkInversions.isEmpty()) {
        return
      }

      val inversion = chunkInversions.removeAt(0)
      plugin.server.scheduler.runTaskLater(plugin, { ->
        inversion.revert()
        scheduleOne()
      }, 5)
    }

    scheduleOne()
  }

  fun invertChunk(chunk: Chunk): ChunkInversion {
    val snapshot = chunk.chunkSnapshot
    forEachChunkPosition { x, z ->
      var sy = chunk.world.minHeight
      var ey = chunk.world.maxHeight
      while (sy != ey) {
        sy++
        ey--
        val targetBlock = chunk.getBlock(x, sy, z)
        val targetBlockData = targetBlock.blockData.clone()
        val nextBlock = chunk.getBlock(x, ey, z)
        val nextBlockData = nextBlock.blockData.clone()
        invertSetBlockData(targetBlock, nextBlockData)
        invertSetBlockData(nextBlock, targetBlockData)
      }
    }
    return ChunkInversion(plugin, snapshot)
  }

  private fun invertSetBlockData(block: Block, data: BlockData) {
    block.setBlockData(data, false)
  }

  class ChunkInversion(val plugin: Plugin, val snapshot: ChunkSnapshot) {
    fun revert() {
      val world = plugin.server.getWorld(snapshot.worldName) ?: return
      val chunk = world.getChunkAt(snapshot.x, snapshot.z)
      chunk.applyChunkSnapshot(snapshot)
    }
  }
}
