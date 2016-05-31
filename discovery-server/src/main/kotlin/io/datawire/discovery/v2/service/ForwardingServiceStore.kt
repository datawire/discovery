package io.datawire.discovery.v2.service

import io.datawire.discovery.v2.model.ServiceKey
import io.datawire.discovery.v2.model.ServiceRecord
import io.datawire.discovery.v2.model.ServiceStore
import io.vertx.core.logging.LoggerFactory


class ForwardingServiceStore(private val delegate: ServiceStore) : ServiceStore by delegate {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun addRecord(record: ServiceRecord) {
    log.debug("Adding service record (tenant: {}, name: {}, addr: {}, ttl: {}s)",
              record.tenant, record.serviceName, record.address, record.timeToLive)

    delegate.addRecord(record)
  }

  override fun removeRecord(key: ServiceKey): Boolean {
    log.debug("Removing service record (tenant: {}, name: {}, addr: {})", key.tenant, key.name, key.address)

    return delegate.removeRecord(key)
  }
}