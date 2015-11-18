package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonProperty


data class ServiceRegistry(
    @JsonProperty("services") val services: Map<String, Set<ServiceEndpoint>>) {
}