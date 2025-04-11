package gay.pizza.foundation.heimdall.tool.commands

import gay.pizza.foundation.heimdall.tool.state.PlayerPositionChangelog
import gay.pizza.foundation.heimdall.tool.util.compose
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import gay.pizza.foundation.heimdall.table.PlayerPositionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

class PlayerPositionExport : CliktCommand(name = "export-player-positions") {
  private val db by requireObject<Database>()

  private val playerIdString by option("--player", help = "Player ID")
  private val startTimeString by option("--start-time", help = "Start Time")
  private val endTimeString by option("--end-time", help = "End Time")

  override fun run() {
    val filter = compose(
      combine = { a, b -> a and b },
      { startTimeString != null } to { PlayerPositionTable.time greaterEq Instant.parse(startTimeString) },
      { endTimeString != null } to { PlayerPositionTable.time lessEq Instant.parse(endTimeString) },
      { playerIdString != null } to { PlayerPositionTable.player eq UUID.fromString(playerIdString) }
    )

    println("time,player,world,x,y,z,pitch,yaw")
    transaction(db) {
      PlayerPositionChangelog.query(db, filter).changes.forEach { change ->
        change.apply {
          println("${time},${player},${world},${x},${y},${z},${pitch},${yaw}")
        }
      }
    }
  }
}
