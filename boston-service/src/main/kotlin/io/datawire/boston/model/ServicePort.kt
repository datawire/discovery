package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty


data class ServicePort @JsonCreator constructor (
    @JsonProperty("port")
    val port: Int,

    @JsonProperty("secure")
    val secure: Boolean
)