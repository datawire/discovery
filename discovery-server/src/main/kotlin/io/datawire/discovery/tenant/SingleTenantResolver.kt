package io.datawire.discovery.tenant

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
data class SingleTenantResolver(
    @JsonProperty("id") val id: String
): TenantResolver {

  override fun resolve() = id
}