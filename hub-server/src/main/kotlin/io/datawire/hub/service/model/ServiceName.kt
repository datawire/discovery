package io.datawire.hub.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue


data class ServiceName(@JsonProperty val name: String) {
  @JsonValue override fun toString(): String = name
}