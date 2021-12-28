package cloud.kubelet.foundation.gjallarhorn.commands

import cloud.kubelet.foundation.gjallarhorn.*
import cloud.kubelet.foundation.heimdall.view.BlockChangeView
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.Instant
import javax.imageio.ImageIO

class BlockLogReplay : CliktCommand("Replay Block Logs", name = "replay-block-log") {
  private val db by requireObject<Database>()
  private val timeAsString by option("--time", help = "Replay Time")
  private val render by option("--render", help = "Enable Render Mode").flag()

  override fun run() {
    val filter = compose(
      combine = { a, b -> a and b },
      { timeAsString != null } to { BlockChangeView.time lessEq Instant.parse(timeAsString) }
    )
    val tracker = BlockStateTracker()

    transaction(db) {
      BlockChangeView.select(filter).orderBy(BlockChangeView.time).forEach { row ->
        val changeIsBreak = row[BlockChangeView.isBreak]
        val x = row[BlockChangeView.x]
        val y = row[BlockChangeView.y]
        val z = row[BlockChangeView.z]
        val block = row[BlockChangeView.block]

        val location = BlockPosition(x.toLong(), y.toLong(), z.toLong())
        if (changeIsBreak) {
          tracker.delete(location)
        } else {
          tracker.place(location, BlockState(block))
        }
      }
    }

    if (render) {
      val image = BlockStateImage()
      tracker.populate(image, offset = tracker.calculateZeroBlockOffset())
      val bufferedImage = image.buildBufferedImage()
      ImageIO.write(bufferedImage, "png", File("top-down.png"))
    } else {
      println("x,y,z,block")
      for ((position, block) in tracker.blocks) {
        println("${position.x},${position.y},${position.z},${block.type}")
      }
    }
  }
}