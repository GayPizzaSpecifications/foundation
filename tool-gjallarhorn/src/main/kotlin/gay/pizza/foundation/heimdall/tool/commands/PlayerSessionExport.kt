package gay.pizza.foundation.heimdall.tool.commands

import gay.pizza.foundation.heimdall.tool.util.compose
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import gay.pizza.foundation.heimdall.table.PlayerSessionTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class PlayerSessionExport : CliktCommand(name = "export-player-sessions") {
  private val db by requireObject<Database>()

  private val playerIdString by option("--player-id", help = "Player ID")
  private val playerNameString by option("--player-name", help = "Player Name")

  override fun run() {
    val filter = compose(
      combine = { a, b -> a and b },
      { playerIdString != null } to { PlayerSessionTable.player eq UUID.fromString(playerIdString) },
      { playerNameString != null } to { PlayerSessionTable.name eq playerNameString!! }
    )

    println("id,player,name,start,end")
    transaction(db) {
      PlayerSessionTable.select(filter).orderBy(PlayerSessionTable.endTime).forEach { row ->
        val id = row[gay.pizza.foundation.heimdall.table.PlayerSessionTable.id]
        val player = row[PlayerSessionTable.player]
        val name = row[PlayerSessionTable.name]
        val start = row[PlayerSessionTable.startTime]
        val end = row[PlayerSessionTable.endTime]

        println("${id},${player},${name},${start},${end}")
      }
    }
  }
}
