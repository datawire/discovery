package io.datawire.discovery.registry

import io.datawire.discovery.registry.ServiceRegistry
import io.datawire.discovery.model.TenantInfo


interface ServiceRegistryRepository {
  fun get(tenant: TenantInfo): ServiceRegistry
  fun put(registry: ServiceRegistry)
}