package io.datawire.hub.auth.cli

import io.datawire.hub.cli.Runners
import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.impl.Arguments
import net.sourceforge.argparse4j.inf.ArgumentParser
import net.sourceforge.argparse4j.inf.ArgumentParserException


class CliRunner() {

  companion object {

    @JvmStatic fun main(arguments: Array<String>) {
      val parser = buildArgParser("1.0.0")

      val runners = Runners(mapOf(
          "server" to { ns -> Server.Factory.build(ns) }
      ))

      try {
        val args = parser.parseArgs(arguments)
        runners.run(args)
      } catch (ex: ArgumentParserException) {
        parser.handleError(ex)
      } catch (any: Throwable) {
        // todo: logging
        throw any
      }
    }

    private fun buildArgParser(version: String): ArgumentParser {
      val parser = ArgumentParsers.newArgumentParser("hub")
          .description("service discovery, routing, and endpoint messaging service")
          .version("\${prog} $version")

      parser.addArgument("--version").action(Arguments.version())

      val commands = parser.addSubparsers().title("commands")
          .description("valid commands")
          .metavar("COMMAND")
          .dest("command")

      val run = commands.addParser("server").help("run the server")
      run.addArgument("config").type(Arguments.fileType().verifyCanRead())

      return parser
    }
  }
}