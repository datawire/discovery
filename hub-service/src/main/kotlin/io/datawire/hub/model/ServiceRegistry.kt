package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonProperty


data class ServiceRegistry(
    @JsonProperty("services") val services: Map<String, Set<ServiceEndpoint>>) {
}