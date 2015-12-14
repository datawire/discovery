package io.datawire.hub.service.model

import com.fasterxml.jackson.annotation.*
import java.net.URI
import java.time.Instant


@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServiceEndpoint @JsonCreator constructor(
    @JsonProperty("name")
    val name: String,

    @JsonProperty("address")
    val address: NetworkAddress,

    @JsonProperty("port")
    val port: ServicePort,

    @JsonProperty("path")
    val path: String?
) {

    @JsonGetter
    fun canonical(): String = "${port.name}://${address.address}:${port.port}${path ?: ""}"

    @JsonIgnore
    val key = ServiceKey(name, toURI())

    fun toURI(): URI = URI(port.name, null, address.address, port.port, null, null, null)
}