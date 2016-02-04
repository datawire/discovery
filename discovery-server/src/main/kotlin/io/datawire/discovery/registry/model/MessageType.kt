package io.datawire.discovery.registry.model

import com.fasterxml.jackson.annotation.JsonProperty

const val DEREGISTER_SERVICE_ALIAS = "deregister"
const val HEARTBEAT_TYPE_ALIAS = "heartbeat"
const val PING_TYPE_ALIAS = "ping"
const val PONG_TYPE_ALIAS = "pong"
const val REGISTER_SERVICE_ALIAS = "register"
const val SUBSCRIBE_ALIAS = "subscribe"
const val ROUTES_ALIAS = "routes"


enum class MessageType() {
  @JsonProperty(DEREGISTER_SERVICE_ALIAS)
  DEREGISTER_SERVICE,

  @JsonProperty(PING_TYPE_ALIAS)
  PING,

  @JsonProperty(PONG_TYPE_ALIAS)
  PONG,

  @JsonProperty(HEARTBEAT_TYPE_ALIAS)
  HEARTBEAT,

  @JsonProperty(REGISTER_SERVICE_ALIAS)
  REGISTER_SERVICE,

  @JsonProperty(SUBSCRIBE_ALIAS)
  SUBSCRIBE,

  @JsonProperty(ROUTES_ALIAS)
  SYNC;
}