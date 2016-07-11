package io.datawire.discovery

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import mdk_discovery.protocol.*
import io.datawire.discovery.config.AuthHandlerConfig
import io.datawire.discovery.config.CorsHandlerConfig
import io.datawire.discovery.model.ServiceRecord
import io.datawire.discovery.model.ServiceStore
import io.datawire.discovery.service.*
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import mdk_protocol.Open


class Discovery : AbstractVerticle() {

  private val logger    = LoggerFactory.getLogger(Discovery::class.java)
  private val hazelcast = initializeHazelcast()

  // TODO(plombardi): This isn't really ideal because we're relying on the Vert.x ClusterManager to be configured.
  //
  // The big problems with this approach:
  //
  // 1. Hazelcast is a Vert.x impl detail
  // 2. Requires the server be started with -cluster parameter.
  //
  // The solution:
  //
  // 1. Expose config options to configure hazelcast properly.
  //
  private fun initializeHazelcast(): HazelcastInstance {
    // allows the -cluster param to be removed in development scenarios. There will be a HZ instance if clustering is enabled.
    return if (Hazelcast.getAllHazelcastInstances().isNotEmpty()) Hazelcast.getAllHazelcastInstances().first() else Hazelcast.newHazelcastInstance()
  }

  private fun configureAuthHandler(router: Router) {
    val config = AuthHandlerConfig(config().getJsonObject("authHandler", JsonObject()))
    when (config.type) {
      "jwt" -> router.route(config.protectPath).handler(config.createJwtAuthHandler(vertx))
      else  -> {
        logger.warn("Not using authentication!")
      }
    }
  }

  private fun configureCorsHandler(router: Router) {
    val config = CorsHandlerConfig(config().getJsonObject("corsHandler", JsonObject()))
    router.route(config.path)
          .handler(config.createCorsHandler())
  }

  override fun start() {
    logger.info("Discovery starting...")

    if (config().isEmpty) {
      logger.error("Could not load Discovery configuration or configuration is empty.")

      // todo(plombardi): Vert.x Shutdown
      //
      // Need to figure out the Vert.x way to abort. #close(), #undeploy(deploymentID()) and System.exit(1)
      // all fail.
    }

    val router = Router.router(vertx)

    router.get("/health").handler { rc -> rc.response().setStatusCode(HttpResponseStatus.OK.code()).end() }

    configureAuthHandler(router)
    configureCorsHandler(router)

    router.route("/ws/v1").handler(DiscoveryConnection())

    val server = vertx.createHttpServer()
    val requestHandler = server.requestHandler { router.accept(it) }

    val host = config().getString("host", "127.0.0.1")
    val port = config().getInteger("port", 52689)
    requestHandler.listen(port, host)

    logger.info("Discovery running (address: {}:{})", host, port)
  }

  inner class DiscoveryConnection() : Handler<RoutingContext> {

    lateinit var serviceStore: ServiceStore

    override fun handle(ctx: RoutingContext) {
      val request = ctx.request()
      val socket  = request.upgrade()

      val tenant = if (config().getJsonObject("authHandler").getString("type", "none") == "none") {
        "default"
      } else {
        ctx.user().principal().getString("aud")
      }

      // TODO: Abstract this bit a bit.
      val replicatedMap = hazelcast.getReplicatedMap<String, ServiceRecord>("discovery.services.$tenant")
      serviceStore = ForwardingServiceStore(ReplicatedServiceStore(replicatedMap))

      //val partitionedMap = hazelcast.getMap<String, ServiceRecord>("discovery.services.$tenant")
      //serviceStore = ForwardingServiceStore(PartitionedServiceStore(partitionedMap))


      vertx.sharedData().getCounter("discovery[${deploymentID()}].$tenant.connections") { getCounter ->
        if (getCounter.succeeded()) {
          getCounter.result().incrementAndGet { increment ->

            if (increment.succeeded() && increment.result() == 1L) {
              logger.info(
                  "First connected client for tenant on this node. Registering service store event listener (node: ${deploymentID()}, tenant: $tenant)")

              val entryListenerId = replicatedMap.addEntryListener(ServicesChangeListener(vertx.eventBus()))
              //val entryListenerId = partitionedMap.addEntryListener(PartitionedServicesStoreListener(vertx.eventBus()), true)

              vertx
                  .sharedData()
                  .getLocalMap<String, String>("discovery.services.event-listeners")
                  .put(tenant, entryListenerId)
            } else if(increment.failed()) {
              logger.error("Could not increment tenant connection counter (tenant: $tenant)")
              socket.close()
            }

            val notificationsAddress = "datawire.discovery.$tenant.services.notifications"
            val notificationsHandler = vertx.eventBus().localConsumer<String>(notificationsAddress)
            notificationsHandler.handler {
              val msg = it.body()
              logger.debug("""Sending Message (client: {})

              {}
              """, socket.textHandlerID(), msg)
              socket.writeFinalTextFrame(msg)
            }

            // A discovery.protocol.clear message is always sent upon a connection being established. The clear message is
            // sent just before the server dumps all known Active services to the client.
            val openMessage = Open()
            socket.writeFinalTextFrame(openMessage.encode())
            socket.writeFinalTextFrame(Clear().encode())
            vertx.executeBlocking<Void>(
                { future ->

                  logger.debug("Sending current registry state to client...")
                  serviceStore.getRecords().forEach { socket.writeFinalTextFrame(it.toActive().encode()) }
                  future.complete()

                }, false, {})

            socket.handler(DiscoveryMessageHandler(tenant, openMessage.version, socket, serviceStore))

            socket.closeHandler {
              if (notificationsHandler.isRegistered) {
                logger.debug("Unregistering client service store notification handler.")
                notificationsHandler.unregister()
              } else {
                logger.warn("Client does not have service store notification handler to unregister.")
              }

              //
              // @Note(Plombardi, 2016-07-08): This code below is probably unncessary.
              //
              // Code decrements and removes a tenant's listener on this node if the tenant count reaches none, but
              // realistically there will likely always be at least one tenant connected to the server no matter what.
              //
              // Re-evaluate the necessity of this code sometime in 2016-08.
              //

//              vertx.sharedData().getCounter("discovery[${deploymentID()}].$tenant.connections") { getCounter ->
//                if (getCounter.succeeded()) {
//                  getCounter.result().decrementAndGet { decrement ->
//
//                    if (decrement.succeeded() && decrement.result() == 0L) {
//                      logger.info(
//                          "Last connected client for tenant on this node. Unregistering service store event listener (node: ${deploymentID()}, tenant: $tenant)")
//
//                      val entryListenerId = vertx.sharedData()
//                          .getLocalMap<String, String>("discovery.services.event-listeners")
//                          .get(tenant)
//
//                      replicatedMap.removeEntryListener(entryListenerId)
//                      //partitionedMap.removeEntryListener(entryListenerId)
//
//
//                    } else if(decrement.failed()) {
//                      logger.error("Could not decrement tenant connection counter (tenant: $tenant)")
//                    }
//                  }
//                }
//              }
            }
          }
        } else {
          logger.error("Failed to get tenant connection counter... possible clustering issue.")
          socket.close()
        }
      }
    }
  }
}