package io.datawire.discovery.registry

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.ReplicatedMap
import io.datawire.discovery.registry.model.Endpoint
import io.datawire.discovery.registry.model.ServiceKey
import io.datawire.discovery.registry.model.ServiceRecord
import io.vertx.core.logging.LoggerFactory
import java.time.Instant
import java.util.concurrent.TimeUnit


class ReplicatedRoutingTable(val hazelcast: HazelcastInstance): RoutingTable {

  private val log = LoggerFactory.getLogger(ReplicatedRoutingTable::class.java)

  init {
    log.info("initializing replicated routing table")
  }

  private fun getRoutingTable(tenant: String): ReplicatedMap<ServiceKey, ServiceRecord> {
    return hazelcast.getReplicatedMap<ServiceKey, ServiceRecord>("services.$tenant")
  }

  override fun contains(key: ServiceKey): Boolean {
    val routes = getRoutingTable(key.tenant)
    return routes.contains(key)
  }

  override fun updateLastContactTime(key: ServiceKey) {
    contains(key)
  }

  override fun get(key: ServiceKey): ServiceRecord? {
    return getRoutingTable(key.tenant)[key]
  }

  override fun addService(key: ServiceKey, endpoint: Endpoint): Boolean {
    log.debug("adding service -> [$key]")
    // todo: replicated maps dont support auto-eviction. this will require a workaround.
    return getRoutingTable(key.tenant).put(key, ServiceRecord(endpoint, Instant.now()), 30, TimeUnit.SECONDS) == null
  }

  override fun removeService(key: ServiceKey): Boolean {
    log.debug("remove service -> [$key]")
    return getRoutingTable(key.tenant).remove(key) != null
  }

  override fun mapNamesToEndpoints(tenant: String): Map<String, Set<Endpoint>> {
    val routingTable = getRoutingTable(tenant)
    return routingTable.entries.fold(linkedMapOf<String, Set<Endpoint>>()) { result, entry ->
      val record = entry.value!!

      if (result.putIfAbsent(entry.key.name, hashSetOf(record.endpoint)) != null) {
        (result[entry.key.name] as MutableSet).add(record.endpoint)
      }

      result
    }
  }
}