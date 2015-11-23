package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Message sent when a service connects and sends information about itself into the service registry.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


class ServiceRegistration @JsonCreator constructor(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("endpoint")
    val endpoint: ServiceEndpoint,

    @JsonProperty("ttl")
    val ttl: Long
): Message