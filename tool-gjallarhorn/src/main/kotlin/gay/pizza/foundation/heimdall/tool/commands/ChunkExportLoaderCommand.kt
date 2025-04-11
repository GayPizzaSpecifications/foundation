package gay.pizza.foundation.heimdall.tool.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import gay.pizza.foundation.heimdall.tool.export.ChunkExportLoader
import gay.pizza.foundation.heimdall.tool.export.CombinedChunkFormat
import gay.pizza.foundation.heimdall.tool.state.BlockExpanse
import gay.pizza.foundation.heimdall.tool.state.BlockLogTracker
import gay.pizza.foundation.heimdall.tool.state.ChangelogSlice
import gay.pizza.foundation.heimdall.tool.util.savePngFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.exposed.sql.Database

class ChunkExportLoaderCommand : CliktCommand(name = "chunk-export-loader") {
  private val db by requireObject<Database>()

  private val exportDirectoryPath by argument("export-directory-path").path()
  private val world by argument("world")
  private val chunkLoadLimit by option("--chunk-limit", help = "Chunk Limit").int()
  private val render by option("--render", help = "Render Top Down Image").enum<ImageRenderType> { it.id }
  private val loadCombinedFormat by option("--load-combined-format").flag()
  private val saveCombinedFormat by option("--save-combined-format").flag()

  override fun run() {
    val combinedFormatFile = exportDirectoryPath.resolve("combined.json").toFile()
    val format = if (loadCombinedFormat) {
      Json.decodeFromStream(CombinedChunkFormat.serializer(), combinedFormatFile.inputStream())
    } else {
      val tracker = BlockLogTracker(isConcurrent = true)
      val loader = ChunkExportLoader(tracker = tracker)
      loader.loadAllChunksForWorld(exportDirectoryPath, world, fast = true, limit = chunkLoadLimit)
      val expanse = BlockExpanse.zeroOffsetAndMax(tracker.calculateZeroBlockOffset(), tracker.calculateMaxBlock())
      val map = tracker.buildBlockMap(expanse.offset)
      CombinedChunkFormat(expanse, map)
    }

    if (render != null) {
      val renderer = render!!.createNewRenderer(format.expanse, db)
      val image = renderer.render(ChangelogSlice.none, format.map)
      image.savePngFile("full.png")
    }

    if (saveCombinedFormat) {
      if (combinedFormatFile.exists()) {
        combinedFormatFile.delete()
      }
      Json.encodeToStream(CombinedChunkFormat.serializer(), format, combinedFormatFile.outputStream())
    }
  }
}
