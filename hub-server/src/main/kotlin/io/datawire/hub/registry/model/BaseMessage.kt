package io.datawire.hub.registry.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import java.util.*

@JsonTypeInfo(
    include = JsonTypeInfo.As.PROPERTY,
    use = JsonTypeInfo.Id.NAME,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DeregisterServiceRequest::class, name = DEREGISTER_SERVICE_ALIAS),
    JsonSubTypes.Type(value = HeartbeatNotification::class, name = HEARTBEAT_TYPE_ALIAS),
    JsonSubTypes.Type(value = PingRequest::class, name = PING_TYPE_ALIAS),
    JsonSubTypes.Type(value = PongResponse::class, name = PONG_TYPE_ALIAS),
    JsonSubTypes.Type(value = RegisterServiceRequest::class, name = REGISTER_SERVICE_ALIAS),
    JsonSubTypes.Type(value = SubscribeNotification::class, name = SUBSCRIBE_ALIAS),
    JsonSubTypes.Type(value = RoutesRequest::class, name = ROUTES_ALIAS),
    JsonSubTypes.Type(value = RoutesResponse::class, name = ROUTES_ALIAS)
)
abstract class BaseMessage(val id: UUID, val origin: String) {

  constructor(origin: String): this(UUID.randomUUID(), origin)

  @JsonIgnore(true)
  val receivedTime = Instant.now()
}