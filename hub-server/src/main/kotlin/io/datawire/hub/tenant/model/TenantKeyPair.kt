package io.datawire.hub.tenant.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

/**
 * Represents a tenants Hub access and secret key pair.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


data class TenantKeyPair(@JsonProperty val id: String, @JsonProperty val secret: String) {

  override fun toString(): String {
    return "${javaClass.simpleName}(id=$id, secret=${secret.replaceRange(0, secret.length - 4, "*")})"
  }

  companion object JacksonDeserializer: StdDeserializer<TenantKeyPair>(TenantKeyPair::class.java) {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): TenantKeyPair? {
      parser!!

      return TenantKeyPair("", "")
    }
  }
}