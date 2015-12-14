package io.datawire.hub.tenant

import io.datawire.hub.tenant.model.Tenant
import io.datawire.hub.tenant.model.TenantId
import java.util.concurrent.ConcurrentHashMap

/**
 * Tenant storage that is suitable for use during development and basic testing scenarios because it does not require an
 * external data source.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


final class LocalTenantStore(map: Map<TenantId, Tenant>): TenantStore {

  private val map = ConcurrentHashMap(map)

  constructor(): this(mapOf())

  override fun addTenant(tenant: Tenant) {
    map.putIfAbsent(tenant.id, tenant)
  }

  override fun contains(id: TenantId): Boolean {
    return map.containsKey(id)
  }

  override fun count() = map.size

  override fun getTenant(id: TenantId): Tenant? {
    return map[id]
  }

  override fun removeTenant(tenant: TenantId) {
    map.remove(key = tenant)
  }
}