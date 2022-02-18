package cloud.kubelet.foundation.gjallarhorn

import cloud.kubelet.foundation.gjallarhorn.commands.BlockChangeTimelapseCommand
import cloud.kubelet.foundation.gjallarhorn.commands.ChunkExportLoaderCommand
import cloud.kubelet.foundation.gjallarhorn.commands.PlayerPositionExport
import cloud.kubelet.foundation.gjallarhorn.commands.PlayerSessionExport
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = GjallarhornCommand().subcommands(
  BlockChangeTimelapseCommand(),
  PlayerSessionExport(),
  PlayerPositionExport(),
  ChunkExportLoaderCommand()
).main(args)
