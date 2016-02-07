package io.datawire.discovery.registry

import io.datawire.discovery.registry.model.*
import io.datawire.discovery.tenant.TenantResolver
import io.vertx.core.json.JsonArray
import io.vertx.core.logging.LoggerFactory


class SharedServiceRegistryVerticle(
    tenants: TenantResolver,
    services: ServiceRegistry
): DiscoveryVerticle(tenants, services) {

  private val log = LoggerFactory.getLogger(SharedServiceRegistryVerticle::class.java)

  override fun startDiscovery() {
    router.route("/messages").handler { rc ->
      val request = rc.request()
      val socket = request.upgrade()

      val audience = rc.user().principal().getValue("aud")
      val tenant = if (audience is String) audience else (audience as JsonArray).getString(0)
      val origin = socket.textHandlerID()

      val serviceNotifications = "services.$tenant"
      val notificationConsumer = vertx.eventBus().consumer<String>(serviceNotifications)

      var serviceKey: ServiceKey? = null

      socket.handler { buffer ->
        val (context, message)  = processMessage(tenant, origin, buffer)

        when(message) {
          is DeregisterServiceRequest -> {
            serviceKey?.let {
              if (registry.removeService(it)) {
                val allServices = registry.mapNamesToEndpoints(tenant)
                vertx.eventBus().publish(serviceNotifications, serializeMessage(RoutesResponse("discovery", allServices)))
              }
            }
          }
          is PingRequest -> socket.writeFinalTextFrame(serializeMessage(PongResponse("discovery")))
          is HeartbeatNotification -> {
            serviceKey?.let {
              registry.updateLastContactTime(it)
            }
          }
          is RegisterServiceRequest -> {
            val key = ServiceKey(context.tenant, message.name, message.endpoint)
            if (registry.addService(key, message.endpoint)) {
              serviceKey = key
              val allServices = registry.mapNamesToEndpoints(tenant)
              vertx.eventBus().publish(serviceNotifications, serializeMessage(RoutesResponse("discovery", allServices)))
            }
          }
          is SubscribeNotification -> {
            log.debug("Adding subscriber -> (addr: $serviceNotifications, sub-id: $origin)")
            notificationConsumer.handler {
              socket.writeFinalTextFrame(it.body())
            }
            val allServices = registry.mapNamesToEndpoints(tenant)
            socket.writeFinalTextFrame(serializeMessage(RoutesResponse("discovery", allServices)))
          }
          is RoutesRequest -> {
            val allServices = registry.mapNamesToEndpoints(tenant)
            socket.writeFinalTextFrame(serializeMessage(RoutesResponse("discovery", allServices)))
          }
          else -> {
            log.error("Unknown message type (type: {0})", message.javaClass)
            socket.close() // close the connection; the client doesn't speak our lingo.
          }
        }
      }

      socket.closeHandler {
        if (notificationConsumer.isRegistered) {
          log.debug("Removing sub handler -> $tenant:$origin")
          notificationConsumer.unregister()
        }

      }
    }

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(config().getInteger("port"))
    log.debug("Running server on {0}", config().getInteger("port"))
  }
}