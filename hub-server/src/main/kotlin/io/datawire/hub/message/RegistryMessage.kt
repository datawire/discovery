package io.datawire.hub.message

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.datawire.hub.service.model.ServiceEndpoint


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RegistryMessage.RegistrySync::class, name="sync")
)
sealed class RegistryMessage() {
  class RegistrySync constructor(
      @JsonProperty("services")
      @JsonSerialize(keyAs = String::class)
      val services: Map<String, Set<ServiceEndpoint>>): RegistryMessage()
}