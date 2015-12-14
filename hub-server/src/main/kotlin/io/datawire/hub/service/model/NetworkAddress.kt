package io.datawire.hub.service.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class NetworkAddress @JsonCreator constructor(
    @JsonProperty("host")
    val address: String,

    @JsonProperty("type")
    val type: ProtocolType
)