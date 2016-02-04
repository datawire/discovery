package io.datawire.discovery.registry.model

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonProperty

class RoutesRequest(
    @JsonProperty
    @JacksonInject("origin") origin: String
) : BaseMessage(origin)