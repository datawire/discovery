package io.datawire.discovery.registry

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.listener.EntryAddedListener
import com.hazelcast.map.listener.EntryEvictedListener
import com.hazelcast.map.listener.EntryRemovedListener
import io.datawire.discovery.registry.model.*
import io.datawire.discovery.tenant.TenantResolver
import io.vertx.core.json.JsonArray
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.ConcurrentHashMap


class SharedDiscoveryVerticle(
    tenants: TenantResolver,
    services: RoutingTable,
    val hazelcast: HazelcastInstance
): DiscoveryVerticle(tenants, services) {

  private val log = LoggerFactory.getLogger(SharedDiscoveryVerticle::class.java)
  private val routingTableListeners = ConcurrentHashMap<String, String>()

  override fun start(verticleId: String) {

    val eventBus = vertx.eventBus()

    router.route("/messages").handler { rc ->
      val request = rc.request()
      val socket = request.upgrade()

      val audience = rc.user().principal().getValue("aud")

      val tenantId = if (audience is String) audience else (audience as JsonArray).getString(0)
      configureRoutingTable(tenantId)

      val clientId = socket.textHandlerID()

      val routingTableNotificationsAddress = "routing-table:$tenantId:notifications"
      val routingTableNotifications = eventBus.localConsumer<String>(routingTableNotificationsAddress)

      var serviceKey: ServiceKey? = null
      socket.handler { buffer ->
        val (context, message) = processMessage(tenantId, clientId, buffer)

        when(message) {
          is DeregisterServiceRequest -> {
            serviceKey?.let { registry.removeService(it) } ?: throw IllegalArgumentException("not registered")
          }
          is PingRequest -> socket.writeFinalTextFrame(serializeMessage(PongResponse("discovery")))
          is HeartbeatNotification -> {
            serviceKey?.let { registry.updateLastContactTime(it) } ?: throw IllegalArgumentException("not registered")
          }
          is RegisterServiceRequest -> {
            val key = ServiceKey(context.tenant, message.name, message.endpoint)
            serviceKey = key
            registry.addService(serviceKey!!, message.endpoint)
          }
          is SubscribeNotification -> {
            routingTableNotifications.handler {
              socket.writeFinalTextFrame(it.body())
            }
            sendRoutingTable(tenantId, socket)
          }
          is RoutesRequest -> {
            sendRoutingTable(tenantId, socket)
          }
          else -> {
            rc.fail(1)
          }
        }
      }

      socket.closeHandler {
        if (routingTableNotifications.isRegistered) {
          routingTableNotifications.unregister()
        }

        serviceKey?.let { registry.removeService(it) }
      }

      socket.exceptionHandler {
        // todo: need a true error protocol
        socket.writeFinalTextFrame("""{"error": "${it.message}"}""")
      }
    }

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(config().getInteger("port"))
    log.debug("Running server on {0}", config().getInteger("port"))
  }

  /**
   * Creates a new routing table for a tenant in Hazelcast. This method IS idempotent and a new table WILL NOT be
   * created if a table exists already. Additionally an event handler will be registered on the table to react to table
   * modifications.
   */
  private fun configureRoutingTable(tenant: String) {
    routingTableListeners.computeIfAbsent(tenant) { k ->
      log.info("adding routing table event listener (tenant: {0})", tenant)
      val table      = hazelcast.getMap<ServiceKey, ServiceRecord>("routing-table:$tenant")
      val listenerId = table.addEntryListener(RoutingTableChangeListener(this), false)

      log.debug("added routing table event listener (listener: {0})", listenerId)
      listenerId
    }
  }

  class RoutingTableChangeListener(private val discovery: DiscoveryVerticle) :
      EntryAddedListener<ServiceKey, ServiceRecord>,
      EntryRemovedListener<ServiceKey, ServiceRecord>,
      EntryEvictedListener<ServiceKey, ServiceRecord> {

    private val log = LoggerFactory.getLogger(RoutingTableChangeListener::class.java)

    override fun entryAdded(event: EntryEvent<ServiceKey, ServiceRecord>?) {
      log.debug("route added (key: {0}}", event!!.key)
      discovery.publishRoutingTable(event.key.tenant)
    }

    override fun entryRemoved(event: EntryEvent<ServiceKey, ServiceRecord>?) {
      log.debug("route removed (key: {0}}", event!!.key)
      discovery.publishRoutingTable(event.key.tenant)
    }

    override fun entryEvicted(event: EntryEvent<ServiceKey, ServiceRecord>?) {
      log.debug("route evicted (key: {0}}", event!!.key)
      discovery.publishRoutingTable(event.key.tenant)
    }
  }
}