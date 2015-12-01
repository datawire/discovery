package io.datawire.hub.cli

import net.sourceforge.argparse4j.inf.Namespace


data class Runners(private val runners: Map<String, (namespace: Namespace) -> Runnable>) {

  fun registerRunner(command: String, factory: (namespace: Namespace) -> Runnable): Runners {
    return this.copy(runners.plus(command to factory))
  }

  fun run(namespace: Namespace) {
    val command = namespace.getString("command")
    val runner  = runners[command]?.invoke(namespace)
    runner?.run() ?: throw IllegalArgumentException("Runner not registered (command: $command)")
  }
}