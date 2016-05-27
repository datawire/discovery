package io.datawire.discovery.v2

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import discovery.protocol.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext


class DiscoveryVerticle : AbstractVerticle() {

  private val logger    = LoggerFactory.getLogger(DiscoveryVerticle::class.java)
  private val hazelcast = Hazelcast.newHazelcastInstance()

  class QuarkBugWorkaround : DiscoHandler {
    override fun onActive(active: Active?) = throw UnsupportedOperationException()
    override fun onExpire(expire: Expire?) = throw UnsupportedOperationException()
    override fun onClear(reset: Clear?)    = throw UnsupportedOperationException()
  }

  override fun start() {
    QuarkBugWorkaround()

    val router = Router.router(vertx)

    router.route("/").handler(DiscoConnection())

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(52689)
  }

  inner class DiscoConnection() : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
      val request = ctx.request()
      val socket  = request.upgrade()
      val tenant  = "none"

      vertx.sharedData().getCounter("discovery[${deploymentID()}].$tenant.connections") { getCounter ->
        if (getCounter.succeeded()) {
          getCounter.result().incrementAndGet { increment ->

            if (increment.succeeded() && increment.result() == 1L) {
              logger.info(
                  "First connected client for tenant on this node. Registering services change listener (node: ${deploymentID()}, tenant: $tenant)")

              val map = hazelcast.getReplicatedMap<String, String>("discovery.services.$tenant")
              val entryListenerId = map.addEntryListener(ServicesChangeListener())

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

      // A discovery.protocol.clear message is always sent upon a connection being established.
      socket.writeFinalTextFrame(Clear().encode())

      socket.handler { buffer ->
        val event = DiscoveryEvent.decode(buffer.toString(Charsets.UTF_8))
        handle(tenant, event)
      }

      socket.closeHandler {
        if (notificationsHandler.isRegistered) {
          notificationsHandler.unregister()
        }
      }
    }

    fun handle(tenant: String, event: DiscoveryEvent) {
      println("is my logger working?")
      logger.debug("Handling {} event (tenant: {})", event.javaClass.simpleName, tenant)
      when (event) {
        is Active -> onActive(tenant, event)
        is Expire -> onExpire(tenant, event)
        is Clear  -> onClear(tenant, event)
        else      -> throw UnsupportedOperationException("TODO: ERROR MESSAGE")
      }
    }

    fun onActive(tenant: String, active: Active) {

    }

    fun onExpire(tenant: String, expire: Expire) {

    }

    fun onClear(tenant: String, reset: Clear) {

    }
  }
}