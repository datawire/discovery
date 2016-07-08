package io.datawire.discovery.service

import io.datawire.discovery.model.ServiceKey
import io.datawire.discovery.model.ServiceRecord
import io.datawire.discovery.model.ServiceStore
import io.vertx.core.logging.LoggerFactory


class ForwardingServiceStore(private val delegate: ServiceStore) : ServiceStore by delegate {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun addRecord(record: ServiceRecord) {
    log.debug("Adding service record (key: {}, ttl: {}s, count: {})", record.key, record.timeToLive, delegate.size)
    delegate.addRecord(record)
    log.debug("Added service record (key: {}, count: {})", record.key, delegate.size)
  }

  override fun removeRecord(key: ServiceKey): Boolean {
    log.debug("Removing service record (key: {}, count{})", key, delegate.size)
    val removed = delegate.removeRecord(key)
    log.debug("Removed service record (key: {}, count: {})", key, delegate.size)

    return removed
  }
}