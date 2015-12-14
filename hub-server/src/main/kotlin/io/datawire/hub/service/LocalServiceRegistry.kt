package io.datawire.hub.service

import io.datawire.hub.service.model.ServiceEndpoint
import io.datawire.hub.service.model.ServiceKey
import io.datawire.hub.service.model.ServiceName
import io.datawire.hub.service.model.ServiceRecord
import io.vertx.core.logging.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class LocalServiceRegistry: ServiceRegistry {

  private val log = LoggerFactory.getLogger(LocalServiceRegistry::class.java)

  private val services: ConcurrentMap<ServiceKey, ServiceRecord> = ConcurrentHashMap()

  override val size = services.size

  override fun contains(key: ServiceKey): Boolean {
    return services.containsKey(key)
  }

  override fun addService(key: ServiceKey, endpoint: ServiceEndpoint): Boolean {
    log.debug("add service    -> [$key]")
    return services.putIfAbsent(key, ServiceRecord(endpoint, Instant.now())) != null
  }

  override fun removeService(key: ServiceKey): Boolean {
    log.debug("remove service -> [$key]")
    return services.remove(key) != null
  }

  override fun updateLastContactTime(key: ServiceKey) {
    log.debug("heartbeat      -> [$key]")
    services.computeIfPresent(key, { k, r -> r.copy(lastHeartbeat = Instant.now()) })
  }

  override fun mapNamesToEndpoints(): Map<ServiceName, Set<ServiceEndpoint>> {
    return services.entries.fold(linkedMapOf<ServiceName, Set<ServiceEndpoint>>()) { result, entry ->
      val record = entry.value!!

//      if (result.putIfAbsent(, hashSetOf(record.endpoint)) != null) {
//        (result[entry.key.name] as MutableSet).add(record.endpoint)
//      }

      result
    }
  }
}