package io.datawire.hub.service

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import io.datawire.hub.event.RegistryEvent
import io.datawire.hub.jwt.QueryJWTAuthHandler
import io.datawire.hub.message.RegistryMessage
import io.datawire.hub.service.model.ServiceKey
import io.datawire.hub.service.model.ServiceName
import io.datawire.hub.tenant.model.TenantId
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import java.util.concurrent.ConcurrentHashMap


class LocalServiceRegistryVerticle(private val jwt: JWTAuth,
                                   private val jsonMapper: ObjectMapper,
                                   private val services: ServiceRegistry,
                                   private val tenant: TenantId): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(LocalServiceRegistryVerticle::class.java)

  private val publishers = ConcurrentHashMap<String, ServiceKey>()
  private val subscribers = ConcurrentHashMap<String, ServerWebSocket>()

  override fun start() {
    log.info("starting service registry... (tenant: {0}, services: {1})", tenant, services.size)

    val router = Router.router(vertx)
    val jwtHandler = QueryJWTAuthHandler(jwt, tenant, null)
    router.route("/*").handler(jwtHandler)
    router.route("/").handler { rc ->
      val request = rc.request()
      val ws = request.upgrade()
      logSocket(ws)

      val clientId = ws.textHandlerID()
      ws.exceptionHandler(ExceptionHandler(ws))

      ws.handler { buf ->
        val event = createEvent(ws, buf)
        handleEvent(ws, event)
      }

      ws.closeHandler {
        log.debug("client disconnected                    -> (client: {0})", clientId)
        subscribers.remove(clientId)
        publishers[clientId]?.let { serviceKey ->
          services.removeService(serviceKey)
          log.debug("removed disconnected client service  -> (client: {0}, service: {1})", clientId, serviceKey)
          broadcastServices()
        }
      }
    }

    val server = vertx.createHttpServer()

    server.requestHandler { router.accept(it) }.listen(config().getInteger("port"))
  }

  private fun createEvent(webSocket: ServerWebSocket, buffer: Buffer): RegistryEvent {
    return RegistryEvent.fromJson(webSocket, buffer)
  }

  private fun handleEvent(webSocket: ServerWebSocket, event: RegistryEvent) {
    var modifiedRegistry = false

    when(event) {
      is RegistryEvent.AddServiceEndpointEvent -> {
        log.debug("received add endpoint request          -> (client: {0}, service: {1})", event.clientId)
        val key = ServiceKey(tenant, ServiceName(event.endpoint.name), event.endpoint.toURI())
        publishers.put(event.clientId, key)
        modifiedRegistry = services.addService(key, event.endpoint)
      }
      is RegistryEvent.Echo -> {
        log.debug("received echo                          -> (client: {0})", event.clientId)
        webSocket.writeFinalTextFrame(jsonMapper.writeValueAsString(event))
      }
      is RegistryEvent.Heartbeat -> {
        publishers[event.clientId]?.let {
          log.debug("received heartbeat                   -> (client: {0}, service: {1})", event.clientId, it)
          services.updateLastContactTime(it)
        }
      }
      is RegistryEvent.RemoveServiceEndpointEvent -> {
        publishers[event.clientId]?.let {
          log.debug("received remove endpoint request     -> (client: {0}, service: {1})", event.clientId, it)
          modifiedRegistry = services.removeService(it)
        }
      }
      is RegistryEvent.QueryRegistry -> {
        log.debug("received synchronization request       -> (client: {0})", event.clientId)
        syncStateToClient(webSocket)
      }
      is RegistryEvent.Subscribe -> {
        log.debug("received subscription request          -> (client: {0})", event.clientId)
        subscribers.put(event.clientId, webSocket)
        syncStateToClient(webSocket)
      }
    }

    if (modifiedRegistry) {
      log.debug("services registry modified; syncing      -> (modifier: {0})", event.clientId)
      broadcastServices()
    }
  }

  private fun logSocket(socket: ServerWebSocket) {
    log.debug("""
--[ WebSocket Connection ]------------------------------------------------------
       ws path : ${socket.path()}
        ws uri : ${socket.uri()}
 ws local-addr : ${socket.localAddress()}
ws remote-addr : ${socket.remoteAddress()}
 connection id :   text --> ${socket.textHandlerID()} <*>
                 binary --> ${socket.binaryHandlerID()}
--------------------------------------------------------------------------------
""")
  }

  private fun broadcastServices() {
    val services = services.mapNamesToEndpoints()
    val json = jsonMapper.writeValueAsString(RegistryMessage.RegistrySync(services))
    broadcast(json)
  }

  private fun broadcast(data: String) {
    for (sub in subscribers.values) {
      syncStateToClient(sub, data)
    }
  }

  private fun send(client: ServerWebSocket, data: String) {
    log.debug("Sending message to client (id: ${client.textHandlerID()})")
    client.writeFinalTextFrame(data)
  }

  private fun syncStateToClient(client: ServerWebSocket, data: String) {
    send(client, data)
  }

  private fun syncStateToClient(client: ServerWebSocket) {
    val services = services.mapNamesToEndpoints()
    val msg = RegistryMessage.RegistrySync(services)
    send(client, jsonMapper.writeValueAsString(msg))
  }

  class ExceptionHandler(private val webSocket: ServerWebSocket): Handler<Throwable> {
    override fun handle(event: Throwable?) {
      when(event) {
        is JsonParseException -> {
          webSocket.writeFinalTextFrame("""{"error": "generic client"}""")
        }
        else -> {
          webSocket.writeFinalTextFrame("""{"error": "generic server"}""")
        }
      }
    }
  }
}