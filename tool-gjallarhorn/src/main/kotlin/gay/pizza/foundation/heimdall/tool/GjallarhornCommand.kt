package gay.pizza.foundation.heimdall.tool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.time.Duration

class GjallarhornCommand : CliktCommand() {
  private val jdbcConnectionUrl by option("-c", "--connection-url", help = "JDBC Connection URL")
    .default("jdbc:postgresql://localhost/heimdall")

  private val jdbcConnectionUsername by option("-u", "--connection-username", help = "JDBC Connection Username")
    .default("heimdall")

  private val jdbcConnectionPassword by option("-p", "--connection-password", help = "JDBC Connection Password")
    .default("heimdall")

  private val dbPoolSize by option("--db-pool-size", help = "JDBC Pool Size").int().default(8)

  override fun run() {
    val pool = HikariDataSource(HikariConfig().apply {
      jdbcUrl = jdbcConnectionUrl
      username = jdbcConnectionUsername
      password = jdbcConnectionPassword
      minimumIdle = dbPoolSize / 2
      maximumPoolSize = dbPoolSize
      idleTimeout = Duration.ofMinutes(5).toMillis()
      maxLifetime = Duration.ofMinutes(10).toMillis()
      schema = "heimdall"
    })
    val db = Database.connect(pool)
    currentContext.findOrSetObject { db }
  }
}
