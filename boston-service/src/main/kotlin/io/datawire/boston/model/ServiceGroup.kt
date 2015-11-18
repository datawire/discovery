package io.datawire.boston.model

import java.net.URI

/**
 * Represents a group of registered services identified by a common name.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


data class ServiceGroup(val name: String, val addresses: Set<ServiceEndpoint>) {

}