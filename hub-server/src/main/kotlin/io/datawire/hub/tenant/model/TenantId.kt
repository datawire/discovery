package io.datawire.hub.tenant.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The key used to refer to a tenant within the system.
 *
 * @param id the value of the tenant key.
 * @author Philip Lombardi <plombardi@datawire.io>
 */
data class TenantId @JsonCreator constructor(@JsonProperty("id") val id: String) {
  override fun toString() = id

  companion object {
    @JvmStatic fun fromString(str: String): TenantId {
      return TenantId(str)
    }
  }
}