package io.datawire.discovery.registry

import io.datawire.discovery.registry.model.*
import io.vertx.core.logging.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.properties.Delegates


class LocalServiceRegistry: ServiceRegistry {

  private val log = LoggerFactory.getLogger(LocalServiceRegistry::class.java)

  private val services: ConcurrentMap<ServiceKey, ServiceRecord> = ConcurrentHashMap()

  override val size: Int
    get() = services.size

  override fun contains(key: ServiceKey): Boolean {
    return services.containsKey(key)
  }

  override fun addService(key: ServiceKey, endpoint: Endpoint): Boolean {
    log.debug("add service    -> [$key]")
    return services.putIfAbsent(key, ServiceRecord(endpoint, Instant.now())) == null
  }

  override fun get(key: ServiceKey): ServiceRecord? {
    return services[key]
  }

  override fun removeService(key: ServiceKey): Boolean {
    log.debug("remove service -> [$key]")
    return services.remove(key) != null
  }

  override fun updateLastContactTime(key: ServiceKey) {
    if (services.computeIfPresent(key, { k, r -> r.copy(lastHeartbeat = Instant.now()) }) != null) {
      log.debug("heartbeat    -> [$key]")
    }
  }

  override fun mapNamesToEndpoints(tenant: String): Map<String, Set<Endpoint>> {
    return services.entries.fold(linkedMapOf<String, Set<Endpoint>>()) { result, entry ->
      val record = entry.value!!

      if (result.putIfAbsent(entry.key.name, hashSetOf(record.endpoint)) != null) {
        (result[entry.key.name] as MutableSet).add(record.endpoint)
      }

      result
    }
  }
}