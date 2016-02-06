package io.datawire.discovery.registry.model

import java.io.Serializable
import java.time.Instant

/**
 * Contains information about a service endpoint as well as additional useful metadata.
 *
 * @param endpoint the service endpoint.
 * @param lastHeartbeat the time of the last heartbeat received from the endpoint.
 * @author Philip Lombardi <plombardi@datawire.io>
 */
data class ServiceRecord(val endpoint: Endpoint, val lastHeartbeat: Instant): Serializable