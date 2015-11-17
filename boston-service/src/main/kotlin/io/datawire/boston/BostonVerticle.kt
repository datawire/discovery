package io.datawire.boston

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory

/**
 * Boston is a service registration and discovery server. The BostonVerticle is designed to be loaded as a Vert.x
 * verticle.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


class BostonVerticle: AbstractVerticle() {

  private val log = LoggerFactory.getLogger(BostonVerticle::class.java)

  override fun start() {
    log.info("booting boston...")

    // todo(plombardi):
    //
    // Vert.x supports clustering which would make our HA story easier. I need to figure out how the
    // shared data structures can be properly shared across the servers (using the clustered map rather than the local
    // map... problem is it exposes a fully async API which kinda sucks to code around). Also i'm not sure how to
    // address the web socket across the cluster.
    val subscribers = linkedListOf<ServerWebSocket>()

    val server = vertx!!.createHttpServer().websocketHandler { event ->
      val clientId = event.textHandlerID()

      println("""
--[ WebSocket Connection ]------------------------------------------------------
       ws path : ${event.path()}
        ws uri : ${event.uri()}
 ws local-addr : ${event.localAddress()}
ws remote-addr : ${event.remoteAddress()}
 connection id : $clientId
--------------------------------------------------------------------------------
""")
      if (event.path().startsWith("/subscribe")) {
        println("---> Subscribe (client: $clientId})")
        subscribers.add(event)
      }

      event.closeHandler { close ->
        println("---> Disconnected (client: $clientId)")
      }

      event.handler { data ->
        val msg = data.toString()
        println("---> Received (msg: $msg)")
        for (sub in subscribers) {
          sub.writeFinalTextFrame("---> msg from($clientId): $msg")
        }
      }
    }

    server.listen(8080)
  }
}