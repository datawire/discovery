package io.datawire.discovery.service

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.core.MapEvent
import io.datawire.discovery.model.ServiceRecord
import io.vertx.core.eventbus.EventBus
import io.vertx.core.logging.LoggerFactory


class ServicesChangeListener(private val eventBus: EventBus) : EntryListener<String, ServiceRecord> {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun entryAdded(event: EntryEvent<String, ServiceRecord>?) {
    val record  = event!!.value
    val message = record.toActive()

    logger.debug("Publishing service record added (key: {})", record.key)
    eventBus.publish("datawire.discovery.${record.key.tenant}.services.notifications", message.encode())
  }

  override fun entryEvicted(event: EntryEvent<String, ServiceRecord>?) {
    val record = event ?: throw NullPointerException("Entry event is null")

    val key = record.key
    val currentValue = event.value
    val mergingValue = event.mergingValue
    val oldValue = event.oldValue

    logger.warn("Record ID ({}), current={}, merging={}, old={}", key, currentValue?.toString(), mergingValue?.toString(), oldValue?.toString())
    val rec2 = record.oldValue
    //val record  = event!!.oldValue

    logger.debug("Publishing service record evicted (key: {})", record.key)
    if (rec2 != null) {
      eventBus.publish("datawire.discovery.${rec2.key.tenant}.services.notifications", rec2.toExpire().encode())
    }
  }

  override fun entryRemoved(event: EntryEvent<String, ServiceRecord>?) {
    val record  = event!!.oldValue
    val message = record.toExpire()

    logger.debug("Publishing service record removed (key: {})", record.key)
    eventBus.publish("datawire.discovery.${record.key.tenant}.services.notifications", message.encode())
  }

  override fun entryUpdated(event: EntryEvent<String, ServiceRecord>?) {
//    val record  = event!!.value
//    val message = record.toActive()
//
//    logger.debug("HANDLING ENTRY UPDATED (key: {})", record.key)
//    eventBus.send("datawire.discovery.${record.key.tenant}.services.notifications", message.encode())
  }

  override fun mapEvicted(event: MapEvent?) = Unit
  override fun mapCleared(event: MapEvent?) = Unit
}