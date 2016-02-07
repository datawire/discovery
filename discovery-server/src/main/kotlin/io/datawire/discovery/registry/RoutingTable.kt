package io.datawire.discovery.registry

import io.datawire.discovery.registry.model.Endpoint
import io.datawire.discovery.registry.model.ServiceKey
import io.datawire.discovery.registry.model.ServiceRecord
import java.util.concurrent.ConcurrentHashMap


interface RoutingTable {

  fun contains(key: ServiceKey): Boolean

  fun updateLastContactTime(key: ServiceKey)
  operator fun get(key: ServiceKey): ServiceRecord?
  fun getService(key: ServiceKey) = get(key)
  fun addService(key: ServiceKey, endpoint: Endpoint): Boolean
  fun removeService(key: ServiceKey): Boolean
  fun mapNamesToEndpoints(tenant: String): Map<String, Set<Endpoint>>
}