package io.datawire.hub.gateway.tenant


interface HubAddressResolver {
  fun resolve(tenant: String): Set<String>
}