package io.datawire.discovery.v2

import discovery.protocol.*
import io.datawire.discovery.v2.model.ServiceKey
import io.datawire.discovery.v2.model.ServiceRecord
import io.datawire.discovery.v2.model.ServiceStore
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.logging.LoggerFactory


class DiscoveryMessageHandler(private val tenant      : String,
                              private val serviceStore: ServiceStore) : DiscoHandler, Handler<Buffer> {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun handle(buffer: Buffer) {
    val event = DiscoveryEvent.decode(buffer.toString(Charsets.UTF_8))
    logger.debug("Handling {} event (tenant: {})", event.javaClass.simpleName, tenant)
    when (event) {
      is Active -> onActive(event)
      is Expire -> onExpire(event)
      is Clear  -> onClear(event)
      else      -> throw UnsupportedOperationException("TODO: ERROR MESSAGE")
    }
  }

  override fun onActive(active: Active) {
    val key = ServiceKey(tenant, active.node)
    val record = ServiceRecord(key,
                               active.node.version,
                               active.ttl.toLong(),
                               active.node.properties.mapValues { it.toString() })


    serviceStore.addRecord(record)
  }

  override fun onExpire(expire: Expire) {
    serviceStore.removeRecord(ServiceKey(tenant, expire.node))
  }

  override fun onClear(reset: Clear?) {
    throw UnsupportedOperationException()
  }
}