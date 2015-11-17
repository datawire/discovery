package io.datawire.boston.model

import java.time.Instant
import java.util.*

/**
 * Message sent when a service connects and sends information about itself into the service registry.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


data class Register(
    val id: UUID,
    val serviceName: String,
    val serviceVersion: String,
    val serviceAddress: String,
    val time: Instant)