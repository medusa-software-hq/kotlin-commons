package software.medusa.git.cli_tool

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import software.medusa.git.Library

private val terminal = Terminal()

class RunCommand : CliktCommand() {
  override fun run() {
    val answer = Library.getAnswer()

    terminal.println("Answer: ${TextColors.brightRed("$answer")}")
  }
}

fun main(
    args: Array<String>,
) {
  RunCommand().main(args)
}
