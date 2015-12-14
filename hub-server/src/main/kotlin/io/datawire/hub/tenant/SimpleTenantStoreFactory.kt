package io.datawire.hub.tenant

import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.hub.tenant.model.Tenant
import io.datawire.hub.tenant.model.TenantId

data class SimpleTenantStoreFactory(
    @JsonProperty val tenants: Map<TenantId, Tenant>
): TenantStoreFactory {

  override fun build(): TenantStore {
    val result = LocalTenantStore(tenants)
    return result
  }
}