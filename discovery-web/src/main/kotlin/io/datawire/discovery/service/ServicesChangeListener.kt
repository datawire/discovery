package io.datawire.discovery.service

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.core.MapEvent
import io.datawire.discovery.event.DiscoveryEvent
import io.datawire.discovery.model.ServiceRecord
import io.datawire.discovery.tenant.TenantReference
import io.vertx.core.eventbus.EventBus
import io.vertx.core.logging.LoggerFactory


class ServicesChangeListener(private val eventBus: EventBus, private val eventBusAddress: String) : EntryListener<String, ServiceRecord> {

  private val logger = LoggerFactory.getLogger(javaClass)

  private fun createTenantReference(record: ServiceRecord): TenantReference {
    return record.properties["datawire_owner"]?.let {
      TenantReference(record.tenant, it) } ?: TenantReference(record.tenant)
  }

  override fun entryAdded(event: EntryEvent<String, ServiceRecord>?) {
    event?.value?.let {
      logger.info("Publishing service record added (key: {})", it.key)

      val tenantReference = createTenantReference(it)
      eventBus.publish("datawire.discovery.${tenantReference.id}.services.notifications", it.toActive().encode())
      eventBus.publish(eventBusAddress, DiscoveryEvent.serviceRegistered(tenantReference, it).json)
    }
  }

  override fun entryEvicted(event: EntryEvent<String, ServiceRecord>?) {
    event?.oldValue?.let {
      logger.info("Publishing service record evicted (key: {})", it.key)

      val tenantReference = createTenantReference(it)
      eventBus.publish("datawire.discovery.${tenantReference.id}.services.notifications", it.toExpire().encode())
      eventBus.publish(eventBusAddress, DiscoveryEvent.serviceRegistered(tenantReference, it).json)
    }
  }

  override fun entryRemoved(event: EntryEvent<String, ServiceRecord>?) {
    event?.oldValue?.let {
      logger.info("Publishing service record removed (key: {})", it.key)

      val tenantReference = createTenantReference(it)
      eventBus.publish("datawire.discovery.${tenantReference.id}.services.notifications", it.toExpire().encode())
      eventBus.publish(eventBusAddress, DiscoveryEvent.serviceExpired(tenantReference, it).json)
    }
  }

  override fun entryUpdated(event: EntryEvent<String, ServiceRecord>?) { }
  override fun mapEvicted(event: MapEvent?) = Unit
  override fun mapCleared(event: MapEvent?) = Unit
}