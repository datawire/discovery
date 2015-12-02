package io.datawire.hub

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.core.ServiceRegistry
import io.datawire.hub.event.RegistryEvent
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory


class ServiceRegistry : AbstractVerticle() {

  private val log = LoggerFactory.getLogger(ServiceRegistry::class.java)

  override fun start() {
    log.info("Hub Verticle Registered1")

    var clients = hashSetOf<String>()

    val objectMapper = ObjectMapper().registerKotlinModule()
    val registry = ServiceRegistry(objectMapper, vertx.eventBus())
    registry.run()

    webSocket { ws ->
      logSocket(ws)

      if (!ws.path().startsWith("/v1/services")) {
        ws.reject()
        return@webSocket
      }

      log.debug("adding services registry client (id: ${ws.textHandlerID()})")
      clients.add(ws.textHandlerID())

      ws.handler { buf ->
        val rawMessage = buf.toString("utf-8")
        log.info("""
--[ WebSocket Message ]---------------------------------------------------------
$rawMessage
--------------------------------------------------------------------------------
""")

        val message = objectMapper.readerFor(RegistryEvent::class.java).with(InjectableValues.Std()
            .addValue("hub.clientId", ws.textHandlerID())).readValue<RegistryEvent>(rawMessage)

        when(message) {
          is RegistryEvent.Echo -> ws.writeFinalTextFrame(objectMapper.writeValueAsString(message))
        }

        when(message) {
          is RegistryEvent.Subscribe -> log.info("Subscribe Message (id: ${message.id}, client: ${message.clientId})")
        }
      }

      ws.closeHandler {
        log.debug("removing services registry client (id: ${ws.textHandlerID()})")
        clients.remove(ws.textHandlerID())
      }
    }
  }

  private fun webSocket(func: (ServerWebSocket) -> Unit) {
    vertx.createHttpServer().websocketHandler(func).listen(config().getInteger("port"))
  }

  private fun logSocket(socket: ServerWebSocket) {
    log.info("""
--[ WebSocket Connection ]------------------------------------------------------
       ws path : ${socket.path()}
        ws uri : ${socket.uri()}
 ws local-addr : ${socket.localAddress()}
ws remote-addr : ${socket.remoteAddress()}
 connection id :   text --> ${socket.textHandlerID()}
                 binary --> ${socket.binaryHandlerID()}
--------------------------------------------------------------------------------
""")
  }
}