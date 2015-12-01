package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonProperty


class Error(
    @JsonProperty("code") val code: Int
) : Message