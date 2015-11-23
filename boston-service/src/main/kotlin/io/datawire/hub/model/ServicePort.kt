package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @author Philip Lombardi <plombardi@datawire.iu
 */


data class ServicePort @JsonCreator constructor (

    /**
     * Indicates the named use for the port, for example, "http", "https", "ftp" or "gopher".
     */
    @JsonProperty("name")
    val name: String,

    /**
     * Indicates the port value.
     */
    @JsonProperty("port")
    val port: Int,

    /**
     * todo(plombardi): might remove this; not convinced it's useful beyond being metadata that is indicated by the name
     *
     * Indicates whether the port is secured. Security is an implementation detail of the named protocol, for example,
     * if the named protocol is HTTPS then clients will need to support HTTPS.
     */
    @JsonProperty("secure")
    val secure: Boolean
)