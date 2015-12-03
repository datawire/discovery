package io.datawire.hub

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.event.RegistryEvent
import io.datawire.hub.model.ServiceEndpoint
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class ServiceRegistry : AbstractVerticle() {

  private val log = LoggerFactory.getLogger(ServiceRegistry::class.java)

  /**
   * Maps a Client ID -> ServiceEndpoint
   */
  private val services: ConcurrentMap<String, ServiceEndpoint> = ConcurrentHashMap()

  /**
   * Contains a list of subscribers
   */
  private val subscribers: Set<String> = ConcurrentHashMap.newKeySet<String>()

  private val mapper = ObjectMapper().registerKotlinModule()

  override fun start() {
    log.info("Hub Verticle Registered!")

    webSocket { ws ->
      logSocket(ws)

      if (!ws.path().startsWith("/v1/services")) {
        ws.reject()
        return@webSocket
      }

      ws.closeHandler {
        log.debug("removing services registry client (id: ${ws.textHandlerID()})")
        services.remove(ws.textHandlerID())
        (subscribers as MutableSet).remove(ws.textHandlerID())
      }

      log.debug("adding services registry client (id: ${ws.textHandlerID()})")

      ws.handler { buf ->
        val event = RegistryEvent.Factory.fromJson(ws, buf)
        onRegistryEvent(event)
      }
    }
  }

  private fun webSocket(func: (ServerWebSocket) -> Unit) {
    vertx.createHttpServer().websocketHandler(func).listen(config().getInteger("port"))
  }

  private fun onRegistryEvent(event: RegistryEvent) {
    when(event) {
      is RegistryEvent.AddServiceEndpointEvent -> {
        log.debug("Adding service ${event.clientId}, ${event.endpoint}")
        val existing = services.putIfAbsent(event.clientId, event.endpoint)
        if (existing != null) {
          services.replace(event.clientId, existing, event.endpoint)
        }
        broadcastServices()
      }
      is RegistryEvent.RemoveServiceEndpointEvent -> {
        services.remove(event.clientId)
        broadcastServices()
      }
      is RegistryEvent.QueryRegistry -> vertx.eventBus().send(event.clientId, mapper.writeValueAsString(getServices()))
      is RegistryEvent.Subscribe     -> (subscribers as MutableSet).add(event.clientId)
    }
  }

  private fun broadcastServices() {
    val json = mapper.writeValueAsString(getServices())
    broadcast(json)
  }

  private fun broadcast(data: String) {
    for (sub in subscribers) {
      log.debug("Sending message to client (id: $sub)")
      vertx.eventBus().publish(sub, data)
    }
  }

  private fun getServices(): Map<String, Set<ServiceEndpoint>> {
    return services.entries.fold(hashMapOf()) { acc, entry ->
      var endpoints = acc.putIfAbsent(entry.value.name, hashSetOf())
      if (endpoints == null) {
        endpoints = acc[entry.value.name]
      }

      (endpoints as MutableSet).add(entry.value)
      acc
    }
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