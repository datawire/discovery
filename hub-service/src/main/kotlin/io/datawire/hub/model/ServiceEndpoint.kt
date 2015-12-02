package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI


data class ServiceEndpoint @JsonCreator constructor(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("address")
    val address: NetworkAddress,

    @JsonProperty("port")
    val port: ServicePort
) {

    val key = ServiceKey(name, toURI())
    fun toURI(): URI = URI(port.name, null, address.address, port.port, null, null, null)
}