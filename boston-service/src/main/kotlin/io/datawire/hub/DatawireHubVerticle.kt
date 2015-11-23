package io.datawire.hub

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory
import com.fasterxml.jackson.module.kotlin.*
import io.datawire.hub.model.*

/**
 * Boston is a service registration and discovery server. The BostonVerticle is designed to be loaded as a Vert.x
 * verticle.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


class DatawireHubVerticle : AbstractVerticle() {

  private val log = LoggerFactory.getLogger(DatawireHubVerticle::class.java)
  private val mapper = ObjectMapper().registerKotlinModule()

  private val clients  = hashMapOf<String, Client>()
  private val services = hashMapOf<String, MutableSet<ServiceEndpoint>>()

  override fun start() {
    log.info("booting up Boston")

    // todo(plombardi):
    //
    // Vert.x supports clustering which would make our HA story easier. I need to figure out how the
    // shared data structures can be properly shared across the servers (using the clustered map rather than the local
    // map... problem is it exposes a fully async API which kinda sucks to code around). Also i'm not sure how to
    // address the web socket across the cluster.
    //val subscribers = linkedListOf<ServerWebSocket>()

    val server = vertx!!.createHttpServer().websocketHandler { socket ->

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


//
//      if (socket.path().startsWith("/subscribe")) {
//        println("---> Subscribe (client: $clientId})")
//        //subscribers.add(event)
//      }
//
//      socket.closeHandler { close ->
//        println("---> Disconnected (client: $clientId)")
//      }
//
//      socket.handler { data ->
//        //val msg = data.toString()
//        val msg = mapper.readValue<Message>(data.toString())
//        when(msg) {
//          is ServiceRegistration -> {
//            print("""
//  sender = ${msg.sender}
//endpoint = ${msg.endpoint}
//
//""")
//          }
//        }



//        println("---> Received (msg: $msg)")
//        for (sub in subscribers) {
//          sub.writeFinalTextFrame("---> msg from($clientId): $msg")
//        }
//      }
    }

    server.listen(8080)
  }
}