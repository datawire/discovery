package io.datawire.hub.registry.model

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

class HeartbeatNotification(
    @JsonProperty
    @JacksonInject("origin") origin: String
) : BaseMessage(origin)