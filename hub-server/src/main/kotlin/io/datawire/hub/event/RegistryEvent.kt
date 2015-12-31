package io.datawire.hub.event

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.service.model.ServiceEndpoint
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

sealed class RegistryEvent(val id: Int, val clientId: String, val time: Long) {
  class AddServiceEndpointEvent @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("endpoint") val endpoint: ServiceEndpoint,
      @JsonProperty("time") time: Long,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId, time)

  class RemoveServiceEndpointEvent @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("endpoint") val endpoint: ServiceEndpoint,
      @JsonProperty("time") time: Long,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId, time)

  class Subscribe @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("time") time: Long,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId, time)

  class QueryRegistry @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("time") time: Long,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId, time)

  class Heartbeat @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("time") time: Long,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId, time)

  class Echo @JsonCreator constructor(
      @JsonProperty("id") id: Int,
      @JsonProperty("payload") val payload: String,
      @JsonProperty("time") time: Long,
      @JacksonInject("hub.clientId") clientId: String
  ): RegistryEvent(id, clientId, time)

  companion object Factory {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    fun fromJson(socket: ServerWebSocket, data: Buffer): RegistryEvent {
      val json = data.toString("UTF-8")
      val reader = objectMapper
          .readerFor(RegistryEvent::class.java)
          .with(InjectableValues.Std().addValue("hub.clientId", socket.textHandlerID()))

      return reader.readValue(json)
    }
  }
}