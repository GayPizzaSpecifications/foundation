package gay.pizza.foundation.heimdall.tool

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import gay.pizza.foundation.heimdall.tool.commands.*

fun main(args: Array<String>) = GjallarhornCommand().subcommands(
  BlockChangeTimelapseCommand(),
  PlayerSessionExport(),
  PlayerPositionExport(),
  ChunkExportLoaderCommand(),
  GenerateWorldLoadFile()
).main(args)
