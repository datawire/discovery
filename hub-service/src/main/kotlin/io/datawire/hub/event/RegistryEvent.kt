package io.datawire.hub.event

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.model.ServiceEndpoint
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket


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

sealed class RegistryEvent(val id: Int, val clientId: String) {
  class AddServiceEndpointEvent @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("endpoint") val endpoint: ServiceEndpoint,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId)

  class RemoveServiceEndpointEvent @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("endpoint") val endpoint: ServiceEndpoint,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId)

  class Subscribe @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId)

  class QueryRegistry @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId)

  class Heartbeat @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId)

  class Echo @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("payload") val payload: String,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId)

  companion object Factory {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun fromJson(socket: ServerWebSocket, data: Buffer): RegistryEvent {
      val reader = objectMapper
          .readerFor(RegistryEvent::class.java)
          .with(InjectableValues.Std().addValue("hub.clientId", socket.textHandlerID()))

      return reader.readValue(data.toString("UTF-8"))
    }
  }
}