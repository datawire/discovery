package io.datawire.hub.tenant

import io.datawire.hub.tenant.model.Tenant
import io.datawire.hub.tenant.model.TenantId

/**
 * Tenant storage that uses Redis as a backing in-memory storage layer.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


class RedisTenantStore: TenantStore {
  override fun addTenant(tenant: Tenant) {
    throw UnsupportedOperationException()
  }

  override fun contains(id: TenantId): Boolean {
    throw UnsupportedOperationException()
  }

  override fun count(): Int {
    throw UnsupportedOperationException()
  }

  override fun getTenant(id: TenantId): Tenant? {
    throw UnsupportedOperationException()
  }

  override fun removeTenant(tenant: TenantId) {
    throw UnsupportedOperationException()
  }
}