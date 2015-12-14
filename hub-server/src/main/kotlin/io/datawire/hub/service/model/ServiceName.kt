package io.datawire.hub.service.model

import com.fasterxml.jackson.annotation.JsonValue


data class ServiceName(val name: String) {
  @JsonValue fun asString() = name
}