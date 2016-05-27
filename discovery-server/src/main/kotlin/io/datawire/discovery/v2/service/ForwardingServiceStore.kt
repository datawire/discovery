package io.datawire.discovery.v2.service

import io.datawire.discovery.v2.model.ServiceRecord
import io.datawire.discovery.v2.model.ServiceStore
import io.vertx.core.logging.LoggerFactory


class ForwardingServiceStore(private val delegate: ServiceStore) : ServiceStore by delegate {

  private val log = LoggerFactory.getLogger(javaClass)

  override fun addRecord(record: ServiceRecord, ttl: Long) {
    log.debug("Adding service record (name: ${record.key.name}, addr: ${record.key.address} ttl: ${ttl}s)")
    delegate.addRecord(record, ttl)
  }
}