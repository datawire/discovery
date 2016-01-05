package io.datawire.hub.tenant

import com.fasterxml.jackson.annotation.JsonProperty


data class SimpleTenantResolver(@JsonProperty val id: String): TenantResolver {
  override fun resolve() = id
}