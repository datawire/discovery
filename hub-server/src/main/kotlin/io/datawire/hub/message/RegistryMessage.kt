package io.datawire.hub.message

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.datawire.hub.event.RegistryEvent
import io.datawire.hub.service.model.ServiceEndpoint


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RegistryEvent.AddServiceEndpointEvent::class, name="sync")
)
sealed class RegistryMessage(@JsonProperty("type") val type: String) {
  class RegistrySync constructor(
      @JsonProperty("services") val services: Map<String, Set<ServiceEndpoint>>): RegistryMessage("sync")
}