package io.datawire.hub.service

import io.datawire.hub.service.model.ServiceEndpoint
import io.datawire.hub.service.model.ServiceKey


interface ServiceRegistry {

  val size: Int

  fun contains(key: ServiceKey): Boolean

  fun updateLastContactTime(key: ServiceKey)
  fun addService(key: ServiceKey, endpoint: ServiceEndpoint): Boolean
  fun removeService(key: ServiceKey): Boolean
  fun mapNamesToEndpoints(): Map<String, Set<ServiceEndpoint>>
}