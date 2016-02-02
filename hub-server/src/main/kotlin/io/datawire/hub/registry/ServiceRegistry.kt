package io.datawire.hub.registry

import io.datawire.hub.registry.model.Endpoint
import io.datawire.hub.registry.model.ServiceKey
import io.datawire.hub.registry.model.ServiceRecord


interface ServiceRegistry {

  val size: Int

  fun contains(key: ServiceKey): Boolean

  fun updateLastContactTime(key: ServiceKey)
  operator fun get(key: ServiceKey): ServiceRecord?
  fun getService(key: ServiceKey) = get(key)
  fun addService(key: ServiceKey, endpoint: Endpoint): Boolean
  fun removeService(key: ServiceKey): Boolean
  fun mapNamesToEndpoints(): Map<String, Set<Endpoint>>
}