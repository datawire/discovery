package io.datawire.boston.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Message sent when a service connects and sends information about itself into the service registry.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


class ServiceRegistration @JsonCreator constructor(
    override val sender: String,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("endpoint")
    val endpoint: ServiceEndpoint
): Message