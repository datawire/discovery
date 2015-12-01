package io.datawire.hub.cli

import io.datawire.hub.DatawireHubVerticle
import io.datawire.hub.model.Client
import io.datawire.hub.model.ServiceEndpoint
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import net.sourceforge.argparse4j.inf.Namespace

/**
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 * @since 1.0
 */


class Server(
    private val vertx: Vertx
): Runnable {

  private val log = LoggerFactory.getLogger(DatawireHubVerticle::class.java)

  override fun run() {
    log.info("Datawire Hub verticle deployed!")

    val clients  = hashMapOf<String, Client>()
    val services = hashMapOf<String, MutableSet<ServiceEndpoint>>()

    val server = vertx.createHttpServer().websocketHandler { socket ->

      log.info("""
--[ WebSocket Connection ]------------------------------------------------------
       ws path : ${socket.path()}
        ws uri : ${socket.uri()}
 ws local-addr : ${socket.localAddress()}
ws remote-addr : ${socket.remoteAddress()}
 connection id : ${socket.textHandlerID()}
--------------------------------------------------------------------------------
""")
      Client(clients, services, socket)
    }

    server.listen(1234)

    System.`in`.read()
  }

  companion object Factory {
    fun build(namespace: Namespace): Server {
      return Server(Vertx.vertx())
    }
  }
}