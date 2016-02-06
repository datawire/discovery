package io.datawire.discovery.registry

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.discovery.registry.model.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.JWTAuthHandler
import java.nio.charset.Charset


class SharedServiceRegistryVerticle(private val services: ServiceRegistry): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(ServiceRegistryVerticle::class.java)
  private val objectMapper = ObjectMapper().registerKotlinModule()

  override fun start() {
    val router = Router.router(vertx)
    router.get("/health").handler { rc ->
      rc.response().setStatusCode(200).end()
    }

    router.route("/messages").handler { rc ->
      val request = rc.request()
      val socket = request.upgrade()

      val tenant = rc.request().getParam("tenant")
      val origin = socket.textHandlerID()

      val serviceNotifications = "services.$tenant"
      val notificationConsumer = vertx.eventBus().consumer<String>(serviceNotifications)

      var serviceKey: ServiceKey? = null

      socket.handler { buffer ->
        val message  = deserializeMessage(origin, buffer)

        when(message) {
          is DeregisterServiceRequest -> {
            serviceKey?.let {
              if (services.removeService(it)) {
                val allServices = services.mapNamesToEndpoints(tenant)
                vertx.eventBus().publish(serviceNotifications, serializeMessage(RoutesResponse("discovery", allServices)))
              }
            }
          }
          is PingRequest -> socket.writeFinalTextFrame(serializeMessage(PongResponse("discovery")))
          is HeartbeatNotification -> {
            serviceKey?.let {
              services.updateLastContactTime(it)
            }
          }
          is RegisterServiceRequest -> {
            val key = ServiceKey(tenant, message.name, message.endpoint)
            if (services.addService(key, message.endpoint)) {
              serviceKey = key
              val allServices = services.mapNamesToEndpoints(tenant)
              vertx.eventBus().publish(serviceNotifications, serializeMessage(RoutesResponse("discovery", allServices)))
            }
          }
          is SubscribeNotification -> {
            log.debug("Adding subscriber -> (addr: $serviceNotifications, sub-id: $origin)")
            notificationConsumer.handler {
              socket.writeFinalTextFrame(it.body())
            }
          }
          is RoutesRequest -> {
            val allServices = services.mapNamesToEndpoints(tenant)
            socket.writeFinalTextFrame(serializeMessage(RoutesResponse("discovery", allServices)))
          }
          else -> {
            log.error("Unknown message type (type: {0})", message.javaClass)
            socket.close() // close the connection; the client doesn't speak our lingo.
          }
        }


//        log.debug("Received message -> {0}", message)
//        when(message) {
//          "ping" -> {
//            log.debug("Publishing   -> {}", serviceNotifications)
//            vertx.eventBus().publish(serviceNotifications, "pong")
//          }
//          "sub" -> {
//            log.debug("Subscribing  -> $tenant:$origin")
//            notificationConsumer.handler {
//              socket.writeFinalTextFrame(it.body())
//            }
//          }
//        }
      }

      socket.closeHandler {
        if (notificationConsumer.isRegistered) {
          log.debug("Removing sub handler -> $tenant:$origin")
          notificationConsumer.unregister()
        }

      }
    }

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(8080)
    log.debug("Running server on 8080")
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

  private fun getRoutes(tenant: String) {
    vertx.sharedData().getClusterWideMap<String, JsonObject>("") {

    }
  }
//
//  private fun deregister(deregistration: DeregisterServiceRequest) {
//    publishers[deregistration.origin]?.let {
//      log.debug("received remove endpoint request     -> (client: {0}, service: {1})", deregistration.origin, it)
//      if (services.removeService(it)) {
//        broadcastServices()
//      }
//    }
//  }
//
//  private fun disconnect(origin: String) {
//    log.debug("client disconnected                    -> (client: {0})", origin)
//    subscribers.remove(origin)
//    publishers[origin]?.let { serviceKey ->
//      services.removeService(serviceKey)
//      log.debug("removed disconnected client service  -> (client: {0}, service: {1})", origin, serviceKey)
//      broadcastServices()
//    }
//  }
//
//  private fun register(registration: RegisterServiceRequest) {
//    val key = ServiceKey(tenant, registration.name, registration.endpoint)
//    publishers.put(registration.origin, key)
//    if (services.addService(key, registration.endpoint)) {
//      broadcastServices()
//    }
//  }
//
//  private fun heartbeat(heartbeat: HeartbeatNotification) {
//    publishers[heartbeat.origin]?.let {
//      log.debug("received heartbeat                   -> (client: {0}, service: {1})", heartbeat.origin, it)
//      services.updateLastContactTime(it)
//    }
//  }
//
//  private fun subscribe(socket: ServerWebSocket, subscribe: SubscribeNotification) {
//    log.debug("received subscription request          -> (client: {0})", subscribe.origin)
//    subscribers.put(subscribe.origin, socket)
//    syncStateToClient(socket)
//  }
//
//  private fun synchronize(socket: ServerWebSocket, routes: RoutesRequest) {
//    log.debug("received synchronization request       -> (client: {0})", routes.origin)
//    syncStateToClient(socket)
//  }
//
//  private fun handleException(socket: ServerWebSocket, cause: Throwable) {
//    when(cause) {
//      is JsonParseException -> {
//        socket.writeFinalTextFrame("""{"error": "generic client"}""")
//      }
//      else -> {
//        socket.writeFinalTextFrame("""{"error": "generic server"}""")
//      }
//    }
//  }
//
//  private fun handleMessage(socket: ServerWebSocket, message: BaseMessage) {
//    when(message) {
//      is DeregisterServiceRequest -> deregister(message)
//      is PingRequest -> socket.writeFinalTextFrame(serializeMessage(PongResponse("discovery")))
//      is HeartbeatNotification -> heartbeat(message)
//      is RegisterServiceRequest -> register(message)
//      is SubscribeNotification -> subscribe(socket, message)
//      is RoutesRequest -> synchronize(socket, message)
//      else -> {
//        log.error("Unknown message type (type: {0})", message.javaClass)
//        socket.close() // close the connection; the client doesn't speak our lingo.
//      }
//    }
//  }
//
//  private fun logSocket(socket: ServerWebSocket) {
//    log.debug("""
//--[ WebSocket Connection ]------------------------------------------------------
//       ws path : ${socket.path()}
//        ws uri : ${socket.uri()}
// ws local-addr : ${socket.localAddress()}
//ws remote-addr : ${socket.remoteAddress()}
// connection id :   text --> ${socket.textHandlerID()} <*>
//                 binary --> ${socket.binaryHandlerID()}
//--------------------------------------------------------------------------------
//""")
//  }
//
//  private fun broadcastServices() {
//    val services = services.mapNamesToEndpoints()
//    val synchronizeResponse = RoutesResponse("discovery", services)
//    val json = serializeMessage(synchronizeResponse)
//    broadcast(json)
//  }
//
//  private fun broadcast(data: String) {
//    for (sub in subscribers.values) {
//      syncStateToClient(sub, data)
//    }
//  }
//
//  private fun send(client: ServerWebSocket, data: String) {
//    log.debug("Sending message to client (id: ${client.textHandlerID()})")
//    //log.debug("Sending raw payload... {0}", data)
//    client.writeFinalTextFrame(data)
//  }
//
//  private fun syncStateToClient(client: ServerWebSocket, data: String) {
//    send(client, data)
//  }
//
//  private fun syncStateToClient(client: ServerWebSocket) {
//    val services = services.mapNamesToEndpoints()
//    val synchronizeResponse = RoutesResponse("discovery", services)
//    val json = serializeMessage(synchronizeResponse)
//    send(client, json)
//  }
//
//  class ExceptionHandler(private val webSocket: ServerWebSocket): Handler<Throwable> {
//    override fun handle(event: Throwable?) {
//      when(event) {
//        is JsonParseException -> {
//          webSocket.writeFinalTextFrame("""{"error": "generic client"}""")
//        }
//        else -> {
//          webSocket.writeFinalTextFrame("""{"error": "generic server"}""")
//        }
//      }
//    }
//  }
}