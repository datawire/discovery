package io.datawire.hub.event

import com.fasterxml.jackson.annotation.*
import io.datawire.hub.model.ServiceEndpoint


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RegistryEvent.AddServiceEndpointEvent::class, name="add-service"),
    JsonSubTypes.Type(value = RegistryEvent.RemoveServiceEndpointEvent::class, name="remove-service"),
    JsonSubTypes.Type(value = RegistryEvent.Subscribe::class, name="subscribe"),
    JsonSubTypes.Type(value = RegistryEvent.QueryRegistry::class, name="query"),
    JsonSubTypes.Type(value = RegistryEvent.Echo::class, name="echo")
)

open class RegistryEvent(val id: Int) {

  class AddServiceEndpointEvent @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      //@JsonProperty("service") service: String,
      @JsonProperty("endpoint") val endpoint: ServiceEndpoint
  ): RegistryEvent(id)

  class RemoveServiceEndpointEvent @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      //@JsonProperty("service") service: String,
      @JsonProperty("endpoint") val endpoint: ServiceEndpoint
  ): RegistryEvent(id)

  class Subscribe @JsonCreator constructor(
      @JsonProperty("id") id: Int,

      @JacksonInject("hub.clientId") val clientId: String
  ): RegistryEvent(id)

  class QueryRegistry @JsonCreator constructor(
      @JsonProperty("id") id: Int
  ): RegistryEvent(id)

  class Echo @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("payload") val payload: String
  ): RegistryEvent(id)
}