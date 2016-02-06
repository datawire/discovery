package io.datawire.discovery.registry

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import io.datawire.discovery.registry.model.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.JWTAuthHandler
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap


class ServiceRegistryVerticle(private val jwtHandler: JWTAuthHandler,
                              private val objectMapper: ObjectMapper,
                              private val services: ServiceRegistry,
                              private val tenant: String
): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(ServiceRegistryVerticle::class.java)

  private val publishers = ConcurrentHashMap<String, ServiceKey>()
  private val subscribers = ConcurrentHashMap<String, ServerWebSocket>()

  override fun start() {
    log.info("starting service registry... (services: {0})", services.size)

    val router = Router.router(vertx)
    router.get("/health").handler { rc ->
      val response = rc.response()
      response.setStatusCode(200)
      response.end()
    }

    router.route("/messages/*").handler(jwtHandler.setAudience(listOf(tenant)))

    router.route("/messages").handler { rc ->
      val request = rc.request()
      val socket = request.upgrade()
      logSocket(socket)

      val origin = socket.textHandlerID()

      socket.closeHandler { disconnect(origin) }
      socket.exceptionHandler(ExceptionHandler(socket))

      socket.handler { buf ->
        val message = deserializeMessage(origin, buf)
        handleMessage(socket, message)
      }
    }

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(config().getInteger("port"))
  }

  private fun deserializeMessage(origin: String, buffer: Buffer, charset: Charset = Charsets.UTF_8): BaseMessage {
    val injections = InjectableValues.Std().addValue("origin", origin)
    val reader = objectMapper.readerFor(BaseMessage::class.java).with(injections)
    return reader.readValue(buffer.toString(charset.name()))
  }

  private fun serializeMessage(message: BaseMessage, charset: Charset = Charsets.UTF_8): String {
    val writer = objectMapper.writer()
    val json = writer.writeValueAsString(message)
    return Buffer.buffer(json).toString(charset.name())
  }

  private fun deregister(deregistration: DeregisterServiceRequest) {
    publishers[deregistration.origin]?.let {
      log.debug("received remove endpoint request     -> (client: {0}, service: {1})", deregistration.origin, it)
      if (services.removeService(it)) {
        broadcastServices()
      }
    }
  }

  private fun disconnect(origin: String) {
    log.debug("client disconnected                    -> (client: {0})", origin)
    subscribers.remove(origin)
    publishers[origin]?.let { serviceKey ->
      services.removeService(serviceKey)
      log.debug("removed disconnected client service  -> (client: {0}, service: {1})", origin, serviceKey)
      broadcastServices()
    }
  }

  private fun register(registration: RegisterServiceRequest) {
    val key = ServiceKey(tenant, registration.name, registration.endpoint)
    publishers.put(registration.origin, key)
    if (services.addService(key, registration.endpoint)) {
      broadcastServices()
    }
  }

  private fun heartbeat(heartbeat: HeartbeatNotification) {
    publishers[heartbeat.origin]?.let {
      log.debug("received heartbeat                   -> (client: {0}, service: {1})", heartbeat.origin, it)
      services.updateLastContactTime(it)
    }
  }

  private fun subscribe(socket: ServerWebSocket, subscribe: SubscribeNotification) {
    log.debug("received subscription request          -> (client: {0})", subscribe.origin)
    subscribers.put(subscribe.origin, socket)
    syncStateToClient(socket)
  }

  private fun synchronize(socket: ServerWebSocket, routes: RoutesRequest) {
    log.debug("received synchronization request       -> (client: {0})", routes.origin)
    syncStateToClient(socket)
  }

  private fun handleException(socket: ServerWebSocket, cause: Throwable) {
    when(cause) {
      is JsonParseException -> {
        socket.writeFinalTextFrame("""{"error": "generic client"}""")
      }
      else -> {
        socket.writeFinalTextFrame("""{"error": "generic server"}""")
      }
    }
  }

  private fun handleMessage(socket: ServerWebSocket, message: BaseMessage) {
    when(message) {
      is DeregisterServiceRequest -> deregister(message)
      is PingRequest              -> socket.writeFinalTextFrame(serializeMessage(PongResponse("discovery")))
      is HeartbeatNotification    -> heartbeat(message)
      is RegisterServiceRequest   -> register(message)
      is SubscribeNotification    -> subscribe(socket, message)
      is RoutesRequest -> synchronize(socket, message)
      else -> {
        log.error("Unknown message type (type: {0})", message.javaClass)
        socket.close() // close the connection; the client doesn't speak our lingo.
      }
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
    val services = services.mapNamesToEndpoints(tenant)
    val synchronizeResponse = RoutesResponse("discovery", services)
    val json = serializeMessage(synchronizeResponse)
    broadcast(json)
  }

  private fun broadcast(data: String) {
    for (sub in subscribers.values) {
      syncStateToClient(sub, data)
    }
  }

  private fun send(client: ServerWebSocket, data: String) {
    log.debug("Sending message to client (id: ${client.textHandlerID()})")
    //log.debug("Sending raw payload... {0}", data)
    client.writeFinalTextFrame(data)
  }

  private fun syncStateToClient(client: ServerWebSocket, data: String) {
    send(client, data)
  }

  private fun syncStateToClient(client: ServerWebSocket) {
    val services = services.mapNamesToEndpoints(tenant)
    val synchronizeResponse = RoutesResponse("discovery", services)
    val json = serializeMessage(synchronizeResponse)
    send(client, json)
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