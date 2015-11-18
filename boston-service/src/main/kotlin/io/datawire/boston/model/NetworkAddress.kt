package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class NetworkAddress @JsonCreator constructor(
    @JsonProperty("host")
    val address: String,

    @JsonProperty("protocol")
    val protocol: ProtocolType
)