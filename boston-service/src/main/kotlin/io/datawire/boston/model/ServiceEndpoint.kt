package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class ServiceEndpoint @JsonCreator constructor(
    @JsonProperty("address")
    val address: NetworkAddress,

    @JsonProperty("port")
    val port: ServicePort
)