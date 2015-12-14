package io.datawire.hub.tenant

import io.datawire.hub.tenant.model.Tenant
import io.datawire.hub.tenant.model.TenantId


interface TenantStore {

  /**
   * Add a tenant.
   */
  fun addTenant(tenant: Tenant)

  /**
   * Check if a tenant exists in the store already.
   */
  fun contains(tenant: Tenant): Boolean = contains(tenant.id)

  /**
   * Check if a tenant exists in the store already.
   */
  fun contains(id: TenantId): Boolean

  /**
   * Return a count of the currently stored tenants.
   */
  fun count(): Int

  /**
   * Retrieve a tenant by the given [TenantId].
   */
  fun get(id: TenantId) = getTenant(id)

  /**
   * Retrieve a tenant by the given [TenantId].
   */
  fun getTenant(id: TenantId): Tenant?

  /**
   * Remove a tenant by the given [TenantId].
   */
  fun removeTenant(tenant: TenantId)

  /**
   * Remove a tenant by using ID of the given [Tenant].
   */
  fun removeTenant(tenant: Tenant) {
    removeTenant(tenant.id)
  }
}