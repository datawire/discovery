package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "message"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ServiceRegistration::class, name="register"),
    JsonSubTypes.Type(value = ServiceHeartbeat::class, name="heartbeat"),
    JsonSubTypes.Type(value = Subscribe::class, name="subscribe")
)
interface Message {
    @get:JsonProperty("sender") val sender: String
}