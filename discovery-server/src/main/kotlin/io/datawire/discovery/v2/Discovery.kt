package io.datawire.discovery.v2

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import discovery.protocol.*
import io.datawire.discovery.v2.config.AuthHandlerConfig
import io.datawire.discovery.v2.config.CorsHandlerConfig
import io.datawire.discovery.v2.model.ServiceRecord
import io.datawire.discovery.v2.model.ServiceStore
import io.datawire.discovery.v2.service.ForwardingServiceStore
import io.datawire.discovery.v2.service.ReplicatedServiceStore
import io.datawire.discovery.v2.service.ServicesChangeListener
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext


class Discovery : AbstractVerticle() {

  private val logger    = LoggerFactory.getLogger(Discovery::class.java)
  private val hazelcast = initializeHazelcast()

  private fun initializeHazelcast(): HazelcastInstance {
    return Hazelcast.newHazelcastInstance()
  }

  private fun configureAuthHandler(router: Router) {
    val config = AuthHandlerConfig(config().getJsonObject("authHandler", JsonObject()))
    when (config.type) {
      "jwt" -> router.route(config.protectPath).handler(config.createJwtAuthHandler(vertx))
      else  -> return
    }
  }

  private fun configureCorsHandler(router: Router) {
    val config = CorsHandlerConfig(config().getJsonObject("corsHandler", JsonObject()))

    router.route(config.path)
          .handler(config.createCorsHandler())
  }

  override fun start() {
    val router = Router.router(vertx)

    configureAuthHandler(router)
    configureCorsHandler(router)

    router.route("/").handler(DiscoveryConnection())

    val server = vertx.createHttpServer()
    val requestHandler = server.requestHandler { router.accept(it) }

    val host = config().getString("host", "127.0.0.1")
    val port = config().getInteger("port", 52689)
    requestHandler.listen(port, host)

    logger.info("Server running (address: {}:{})", host, port)
  }

  inner class DiscoveryConnection() : Handler<RoutingContext> {

    lateinit var serviceStore: ServiceStore

    override fun handle(ctx: RoutingContext) {
      val request = ctx.request()
      val socket  = request.upgrade()

      val tenant = if (config().getJsonObject("authentication").getString("type", "none") == "none") {
        "default"
      } else {
        ctx.user().principal().getString("aud")
      }

      // TODO: Abstract this bit a bit.
      val replicatedMap = hazelcast.getReplicatedMap<String, ServiceRecord>("discovery.services.$tenant")
      serviceStore = ForwardingServiceStore(ReplicatedServiceStore(replicatedMap))

//      serviceStore.addRecord(ServiceRecord(ServiceKey("foo", "foo-host"), "1.0.0", mapOf()), 20)
//      serviceStore.addRecord(ServiceRecord(ServiceKey("bar", "bar-host"), "1.0.0", mapOf()), 40)
//      serviceStore.addRecord(ServiceRecord(ServiceKey("baz", "baz-host"), "1.0.0", mapOf()), 60)

      vertx.sharedData().getCounter("discovery[${deploymentID()}].$tenant.connections") { getCounter ->
        if (getCounter.succeeded()) {
          getCounter.result().incrementAndGet { increment ->

            if (increment.succeeded() && increment.result() == 1L) {
              logger.info(
                  "First connected client for tenant on this node. Registering service store event listener (node: ${deploymentID()}, tenant: $tenant)")

              val entryListenerId = replicatedMap.addEntryListener(ServicesChangeListener(vertx.eventBus()))

              vertx
                  .sharedData()
                  .getLocalMap<String, String>("discovery.services.event-listeners")
                  .put(tenant, entryListenerId)

            } else if(increment.failed()) {
              logger.error("Could not increment tenant connection counter (tenant: $tenant)")
              socket.close()
            }

          }
        }
      }

      val notificationsAddress = "datawire.discovery.$tenant.services.notifications"
      val notificationsHandler = vertx.eventBus().localConsumer<String>(notificationsAddress)
      notificationsHandler.handler {
        socket.writeFinalTextFrame(it.body())
      }

      // A discovery.protocol.clear message is always sent upon a connection being established. The clear message is
      // sent just before the server dumps all known Active services to the client.
      socket.writeFinalTextFrame(Clear().encode())
      vertx.executeBlocking<Void>(
          {
            future -> serviceStore.getRecords().forEach { socket.writeFinalTextFrame(it.toActive().encode()) }
            future.complete()
          }, false, { })

      socket.handler(DiscoveryMessageHandler(tenant, serviceStore))

      socket.closeHandler {
        if (notificationsHandler.isRegistered) {
          logger.debug("Unregistering client service store notification handler")
          notificationsHandler.unregister()
        }

        vertx.sharedData().getCounter("discovery[${deploymentID()}].$tenant.connections") { getCounter ->
          if (getCounter.succeeded()) {
            getCounter.result().decrementAndGet { decrement ->

              if (decrement.succeeded() && decrement.result() == 0L) {
                logger.info(
                    "Last connected client for tenant on this node. Unregistering service store event listener (node: ${deploymentID()}, tenant: $tenant)")

                val entryListenerId = vertx.sharedData()
                                           .getLocalMap<String, String>("discovery.services.event-listeners")
                                           .get(tenant)

                replicatedMap.removeEntryListener(entryListenerId)

              } else if(decrement.failed()) {
                logger.error("Could not decrement tenant connection counter (tenant: $tenant)")
              }
            }
          }
        }
      }
    }
  }
}