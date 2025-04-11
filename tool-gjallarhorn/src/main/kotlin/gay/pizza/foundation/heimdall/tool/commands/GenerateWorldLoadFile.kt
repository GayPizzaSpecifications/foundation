package gay.pizza.foundation.heimdall.tool.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import gay.pizza.foundation.heimdall.export.ExportedBlock
import gay.pizza.foundation.heimdall.load.ExportedBlockTable
import gay.pizza.foundation.heimdall.load.WorldLoadFormat
import gay.pizza.foundation.heimdall.load.WorldLoadSimpleWorld
import gay.pizza.foundation.heimdall.table.WorldChangeTable
import gay.pizza.foundation.heimdall.tool.state.BlockChangelog
import gay.pizza.foundation.heimdall.tool.state.BlockLogTracker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

class GenerateWorldLoadFile : CliktCommand(name = "generate-world-load") {
  private val db by requireObject<Database>()

  val path by argument("load-format-file").path()

  override fun run() {
    val worlds = mutableMapOf<String, WorldLoadSimpleWorld>()
    val worldChangelogs = BlockChangelog.query(db).splitBy { it.world }
    val worldNames = transaction(db) {
      WorldChangeTable.selectAll()
        .associate { it[WorldChangeTable.toWorld] to it[WorldChangeTable.toWorldName] }
    }

    val blockTable = ExportedBlockTable()

    for ((id, changelog) in worldChangelogs) {
      val tracker = BlockLogTracker()
      tracker.replay(changelog)
      val sparse = tracker.buildBlockMap { ExportedBlock(it.type, it.data) }
      val blocks = sparse.blocks
      worlds[id.toString().lowercase()] = WorldLoadSimpleWorld(
        worldNames[id] ?: "unknown_$id",
        blocks.mapValues { levelOne ->
          levelOne.value.mapValues { levelTwo ->
            levelTwo.value.mapValues { entry ->
              blockTable.index(entry.value)
            }
          }
        }
      )
    }
    val format = WorldLoadFormat(blockTable.blocks, worlds)
    path.deleteIfExists()
    Json.encodeToStream(format, path.outputStream())
  }
}
