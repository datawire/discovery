package io.datawire.discovery.v2

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.core.MapEvent
import io.datawire.discovery.v2.model.ServiceRecord
import io.vertx.core.eventbus.EventBus


class ServicesChangeListener(private val eventBus: EventBus) : EntryListener<String, ServiceRecord> {

  override fun entryAdded(event: EntryEvent<String, ServiceRecord>?) {
    val record  = event!!.value
    val message = record.toActive()
    eventBus.publish("datawire.discovery.${record.key.tenant}.services.notifications", message.encode())
  }

  override fun entryEvicted(event: EntryEvent<String, ServiceRecord>?) {
    val record  = event!!.oldValue
    val message = record.toExpire()
    eventBus.publish("datawire.discovery.${record.key.tenant}.services.notifications", message.encode())
  }

  override fun entryRemoved(event: EntryEvent<String, ServiceRecord>?) {
    val record  = event!!.value
    val message = record.toExpire()
    eventBus.publish("datawire.discovery.${record.key.tenant}.services.notifications", message.encode())
  }

  override fun entryUpdated(event: EntryEvent<String, ServiceRecord>?) = Unit
  override fun mapEvicted(event: MapEvent?) = Unit
  override fun mapCleared(event: MapEvent?) = Unit
}