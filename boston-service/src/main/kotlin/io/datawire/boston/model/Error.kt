package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonProperty


class Error(
    override val sender: String,
    @JsonProperty("code") val code: Int
) : Message