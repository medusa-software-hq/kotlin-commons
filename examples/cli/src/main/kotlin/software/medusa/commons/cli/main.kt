package software.medusa.commons.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.mordant.terminal.Terminal

private val terminal = Terminal()

class RunCommand : CliktCommand() {
  override fun run() {
    terminal.println("Answer: ${42}")
  }
}

fun main(
    args: Array<String>,
) {
  RunCommand().main(args)
}
