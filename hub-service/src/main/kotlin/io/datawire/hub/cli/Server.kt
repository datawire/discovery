package io.datawire.hub.cli

import io.datawire.hub.ServiceRegistry
import io.vertx.core.Vertx
import net.sourceforge.argparse4j.inf.Namespace

/**
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 * @since 1.0
 */


class Server(
    private val vertx: Vertx
): Runnable {

  override fun run() {
    vertx.deployVerticle(ServiceRegistry())
    System.`in`.read()
  }

  companion object Factory {
    fun build(namespace: Namespace): Server {
      return Server(Vertx.vertx())
    }
  }
}