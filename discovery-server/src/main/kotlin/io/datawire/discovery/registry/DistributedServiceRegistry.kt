package io.datawire.discovery.registry

import com.hazelcast.core.HazelcastInstance
import io.datawire.discovery.registry.model.Endpoint
import io.datawire.discovery.registry.model.ServiceKey
import io.datawire.discovery.registry.model.ServiceRecord
import io.vertx.core.logging.LoggerFactory
import java.time.Instant


class DistributedServiceRegistry(val hazelcast: HazelcastInstance): ServiceRegistry {

  private val log = LoggerFactory.getLogger(DistributedServiceRegistry::class.java)

  override val size: Int = 0

  override fun contains(key: ServiceKey): Boolean {
    val map = hazelcast.getMap<ServiceKey, ServiceRecord>("services.${key.tenant}")
    return map.contains(key)
  }

  override fun updateLastContactTime(key: ServiceKey) {
    val map = hazelcast.getMap<ServiceKey, ServiceRecord>("services.${key.tenant}")
    if (map.computeIfPresent(key, { k, r -> r.copy(lastHeartbeat = Instant.now()) }) != null) {
      log.debug("heartbeat    -> [$key]")
    }
  }

  override fun get(key: ServiceKey): ServiceRecord? {
    val map = hazelcast.getMap<ServiceKey, ServiceRecord>("services.${key.tenant}")
    return map[key]
  }

  override fun addService(key: ServiceKey, endpoint: Endpoint): Boolean {
    log.debug("add service    -> [$key]")
    val map = hazelcast.getMap<ServiceKey, ServiceRecord>("services.${key.tenant}")
    return map.putIfAbsent(key, ServiceRecord(endpoint, Instant.now())) == null
  }

  override fun removeService(key: ServiceKey): Boolean {
    log.debug("remove service -> [$key]")
    val map = hazelcast.getMap<ServiceKey, ServiceRecord>("services.${key.tenant}")
    return map.remove(key) != null
  }

  override fun mapNamesToEndpoints(tenant: String): Map<String, Set<Endpoint>> {
    val map = hazelcast.getMap<ServiceKey, ServiceRecord>("services.$tenant")
    return map.entries.fold(linkedMapOf<String, Set<Endpoint>>()) { result, entry ->
      val record = entry.value!!

      if (result.putIfAbsent(entry.key.name, hashSetOf(record.endpoint)) != null) {
        (result[entry.key.name] as MutableSet).add(record.endpoint)
      }

      result
    }
  }
}