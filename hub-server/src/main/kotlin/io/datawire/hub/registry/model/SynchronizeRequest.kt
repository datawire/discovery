package io.datawire.hub.registry.model

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonProperty

class SynchronizeRequest(
    @JsonProperty
    @JacksonInject("origin") origin: String,

    @JsonProperty
    val content: String
) : BaseMessage(origin)