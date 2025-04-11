package gay.pizza.foundation.heimdall.tool.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import gay.pizza.foundation.heimdall.table.BlockChangeTable
import gay.pizza.foundation.heimdall.table.WorldChangeTable
import gay.pizza.foundation.heimdall.tool.render.*
import gay.pizza.foundation.heimdall.tool.state.*
import gay.pizza.foundation.heimdall.tool.util.compose
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.lang.Exception
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor

class BlockChangeTimelapseCommand : CliktCommand("block-change-timelapse") {
  private val db by requireObject<Database>()
  private val timelapseIntervalLimit by option("--timelapse-limit", help = "Timelapse Limit Intervals").int()
  private val timelapseMode by option("--timelapse", help = "Timelapse Mode").enum<TimelapseMode> { it.id }.required()
  private val timelapseSpeedChangeThreshold by option(
    "--timelapse-change-speed-threshold",
    help = "Timelapse Change Speed Threshold"
  ).int()
  private val timelapseSpeedChangeMinimumIntervalSeconds by option(
    "--timelapse-change-speed-minimum-interval-seconds",
    help = "Timelapse Change Speed Minimum Interval Seconds"
  ).int()

  private val render by option("--render", help = "Render Top Down Image").enum<ImageRenderType> { it.id }.required()
  private val renderImageFormat by option("--render-image-format", help = "Render Image Format")
    .enum<ImageFormatType> { it.id }
    .default(ImageFormatType.Png)

  private val fromCoordinate by option("--trim-from", help = "Trim From Coordinate")
  private val toCoordinate by option("--trim-to", help = "Trim To Coordinate")

  private val parallelPoolSize by option("--pool-size", help = "Task Pool Size").int().default(8)
  private val inMemoryRender by option("--in-memory-render", help = "Render Images to Memory").flag()
  private val shouldRenderLoop by option("--loop-render", help = "Loop Render").flag()
  private val quadPixelNoop by option("--quad-pixel-noop", help = "Disable Quad Pixel Render").flag()

  private val filterByWorld by option("--world", help = "World ID or Name").default("world")

  private val logger = LoggerFactory.getLogger(BlockChangeTimelapseCommand::class.java)

  override fun run() {
    if (quadPixelNoop) {
      BlockGridRenderer.globalQuadPixelNoop = true
    }
    val threadPoolExecutor = ScheduledThreadPoolExecutor(parallelPoolSize)
    if (shouldRenderLoop) {
      while (true) {
        perform(threadPoolExecutor)
      }
    } else {
      perform(threadPoolExecutor)
    }
    threadPoolExecutor.shutdown()
  }

  private fun perform(threadPoolExecutor: ScheduledThreadPoolExecutor) {
    val trim = maybeBuildTrim()

    val worldNames = transaction(db) {
      WorldChangeTable.selectAll()
        .associate { it[WorldChangeTable.toWorld] to it[WorldChangeTable.toWorldName] }
    }

    var world: UUID? = null
    try {
      world = UUID.fromString(filterByWorld)
    } catch (_: Exception) {}
    if (world == null) {
      world = worldNames.entries.firstOrNull { it.value == filterByWorld }?.key
    }

    if (world == null) {
      throw RuntimeException("World '${filterByWorld}' not found.")
    }

    val filter = compose(
      combine = { a, b -> a and b },
      { trim?.first?.x != null } to { BlockChangeTable.x greaterEq trim!!.first.x.toDouble() },
      { trim?.first?.z != null } to { BlockChangeTable.z greaterEq trim!!.first.z.toDouble() },
      { trim?.second?.x != null } to { BlockChangeTable.x lessEq trim!!.second.x.toDouble() },
      { trim?.second?.z != null } to { BlockChangeTable.z lessEq trim!!.second.z.toDouble() },
      { true } to { BlockChangeTable.world eq world }
    )

    val changelog = BlockChangelog.query(db, filter)
    logger.info("Block Changelog: ${changelog.changes.size} changes")
    val timelapse = BlockMapTimelapse<BufferedImage>()
    var slices = changelog.calculateChangelogSlices(timelapseMode.interval, timelapseIntervalLimit)

    if (timelapseSpeedChangeThreshold != null && timelapseSpeedChangeMinimumIntervalSeconds != null) {
      val minimumInterval = Duration.ofSeconds(timelapseSpeedChangeMinimumIntervalSeconds!!.toLong())
      val blockChangeThreshold = timelapseSpeedChangeThreshold!!

      slices = changelog.splitChangelogSlicesWithThreshold(blockChangeThreshold, minimumInterval, slices)
    }

    logger.info("Timelapse Slices: ${slices.size} slices")

    val imagePadCount = slices.size.toString().length

    val inMemoryPool = if (inMemoryRender) {
      ConcurrentHashMap<ChangelogSlice, BufferedImage>()
    } else {
      null
    }

    val pool = BlockMapRenderPool(
      changelog = changelog,
      delegate = timelapse,
      createRendererFunction = { expanse -> render.createNewRenderer(expanse, db) },
      threadPoolExecutor = threadPoolExecutor
    ) { slice, result ->
      val speed = slice.sliceRelativeDuration.toSeconds().toDouble() / timelapseMode.interval.toSeconds().toDouble()
      val graphics = result.createGraphics()
      val font = Font.decode("Arial Black").deriveFont(24.0f)
      graphics.color = Color.black
      graphics.font = font
      val context = graphics.fontRenderContext
      val text = String.format("%s @ %.4f speed (1 frame = %s sec)", slice.sliceEndTime, speed, slice.sliceRelativeDuration.toSeconds())
      val layout =
        TextLayout(text, font, context)
      layout.draw(graphics, 60f, 60f)
      graphics.dispose()
      val index = slices.indexOf(slice) + 1
      if (inMemoryRender) {
        inMemoryPool?.put(slice, result)
      } else {
        val suffix = "-${index.toString().padStart(imagePadCount, '0')}"
        renderImageFormat.save(result, "${render.id}${suffix}.${renderImageFormat.extension}")
      }
      logger.info("Rendered Timelapse Slice $index")
    }

    pool.render(slices)
  }

  private fun maybeBuildTrim(): Pair<BlockCoordinate, BlockCoordinate>? {
    if (fromCoordinate == null || toCoordinate == null) {
      return null
    }

    val from = fromCoordinate!!.split(",").map { it.toLong() }
    val to = toCoordinate!!.split(",").map { it.toLong() }

    val fromBlock = BlockCoordinate(from[0], 0, from[1])
    val toBlock = BlockCoordinate(to[0], 0, to[1])
    return fromBlock to toBlock
  }

  @Suppress("unused")
  enum class TimelapseMode(val id: String, val interval: Duration) {
    ByHour("hours", Duration.ofHours(1)),
    ByDay("days", Duration.ofDays(1)),
    ByFifteenMinutes("fifteen-minutes", Duration.ofMinutes(15))
  }
}
