package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ServiceRegistration::class, name="register"),
    JsonSubTypes.Type(value = ServiceHeartbeat::class, name="heartbeat"),
    JsonSubTypes.Type(value = Subscribe::class, name="subscribe")
)
interface Message {}