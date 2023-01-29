package gay.pizza.foundation.heimdall.tool.render

import gay.pizza.foundation.heimdall.table.PlayerPositionTable
import gay.pizza.foundation.heimdall.tool.state.*
import gay.pizza.foundation.heimdall.tool.util.BlockColorKey
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.*

class PlayerLocationShareRenderer(
  val expanse: BlockExpanse,
  val db: Database,
  quadPixelSize: Int = defaultQuadPixelSize
) : BlockGridRenderer(quadPixelSize) {
  private val colorKey = BlockColorKey(mapOf())

  override fun render(slice: ChangelogSlice, map: BlockStateMap): BufferedImage {
    val start = slice.sliceChangeRange.start
    val end = slice.sliceChangeRange.endInclusive

    val playerSparseMap = BlockCoordinateSparseMap<MutableList<UUID>>()
    val allPlayerIds = HashSet<UUID>()
    transaction(db) {
      gay.pizza.foundation.heimdall.table.PlayerPositionTable.select {
        (gay.pizza.foundation.heimdall.table.PlayerPositionTable.time greater start) and
            (gay.pizza.foundation.heimdall.table.PlayerPositionTable.time lessEq end)
      }.forEach {
        val x = it[gay.pizza.foundation.heimdall.table.PlayerPositionTable.x].toLong()
        val y = it[gay.pizza.foundation.heimdall.table.PlayerPositionTable.y].toLong()
        val z = it[gay.pizza.foundation.heimdall.table.PlayerPositionTable.z].toLong()
        val coordinate = expanse.offset.applyAsOffset(BlockCoordinate(x, y, z))
        val player = it[gay.pizza.foundation.heimdall.table.PlayerPositionTable.player]
        playerSparseMap.createOrModify(
          coordinate,
          create = { mutableListOf(player) },
          modify = { players -> players.add(player) })
        allPlayerIds.add(player)
      }
    }

    val colorOfPlayers = allPlayerIds.associateWith { colorKey.map(it.toString()) }

    return buildPixelQuadImage(expanse) { g, x, z ->
      val players = playerSparseMap.getVerticalSection(x, z)?.flatMap { it.value }?.distinct()
      if (players != null) {
        setPixelQuad(g, x, z, colorOfPlayers[players.first()]!!)
      } else {
        setPixelQuad(g, x, z, Color.white)
      }
    }
  }
}
