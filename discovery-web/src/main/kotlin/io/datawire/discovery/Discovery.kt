package io.datawire.discovery

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import mdk_discovery.protocol.*
import io.datawire.discovery.config.AuthHandlerConfig
import io.datawire.discovery.config.CorsHandlerConfig
import io.datawire.discovery.mixpanel.MixpanelIntegration
import io.datawire.discovery.model.ServiceRecord
import io.datawire.discovery.model.ServiceStore
import io.datawire.discovery.service.*
import io.datawire.discovery.tenant.TenantReference
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.eventbus.impl.codecs.JsonObjectMessageCodec
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import mdk_protocol.Open
import java.util.*


class Discovery : AbstractVerticle() {

  companion object {
    /**
     * A unique identifier for this Discovery instance.
     */
    val ID: UUID = UUID.randomUUID()

    /**
     * The EventBus address used for events that should only be processed by consumers on this node.
     */
    val LOCAL_EVENT_ADDRESS = "discovery.${ID.toString()}.events"

    /**
     * The EventBus address used for events that should be processed by any consumer within the cluster.
     */
    val CLUSTER_EVENT_ADDRESS = "discovery.cluster.events"
  }

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

    // load the Mixpanel integration if the Mixpanel configuration object is present and it is enabled.
    config().getJsonObject(MixpanelIntegration.CONFIG_KEY)?.let {
      if (it.getBoolean("enabled", false)) {

        it.put("eventBusAddress", LOCAL_EVENT_ADDRESS)

        vertx.deployVerticle(MixpanelIntegration(), DeploymentOptions().setWorker(true).setConfig(it)) { res ->
          if (res.succeeded()) {
            logger.info("Mixpanel integration deploy succeeded")
          } else {
            logger.error("Mixpanel integration deploy failed")
          }
        }
      }
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
        TenantReference("default")
      } else {
        TenantReference(ctx.user().principal().getString("aud"), ctx.user().principal().getString("email"))
      }

      // TODO: Abstract this bit a bit.
      val replicatedMap = hazelcast.getReplicatedMap<String, ServiceRecord>("discovery.services.${tenant.id}")
      serviceStore = ForwardingServiceStore(ReplicatedServiceStore(replicatedMap), vertx.eventBus())

      vertx.sharedData().getCounter("discovery[${deploymentID()}].${tenant.id}.connections") { getCounter ->
        if (getCounter.succeeded()) {
          getCounter.result().incrementAndGet { increment ->

            if (increment.succeeded() && increment.result() == 1L) {
              logger.info(
                  "First connected client for tenant on this node. Registering service store event listener (node: ${deploymentID()}, tenant: $tenant)")

              val entryListenerId = replicatedMap.addEntryListener(ServicesChangeListener(vertx.eventBus(), LOCAL_EVENT_ADDRESS))

              vertx
                  .sharedData()
                  .getLocalMap<String, String>("discovery.services.event-listeners")
                  .put(tenant.id, entryListenerId)
            } else if(increment.failed()) {
              logger.error("Could not increment tenant connection counter (tenant: {})", tenant.id)
              socket.close()
            }

            val notificationsAddress = "datawire.discovery.${tenant.id}.services.notifications"
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