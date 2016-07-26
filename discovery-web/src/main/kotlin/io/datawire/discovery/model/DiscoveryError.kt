package io.datawire.discovery.model

import mdk_protocol.ProtocolError
import java.util.*
import java.util.UUID.*


sealed class DiscoveryError(val id: UUID, val code: String, val title: String, val detail: String) {

  class ProtocolMismatch() :
      DiscoveryError(randomUUID(), "10", "Client-Server Protocol Version Mismatch", "The client and server are speaking incompatible protocol versions.")

  class ClientEventNotAllowed() :
      DiscoveryError(randomUUID(), "11", "Client Event Not Allowed", "The client is not authorized to send the event.")

  class InternalServerError() :
      DiscoveryError(randomUUID(), "1000", "Internal Server Error", "An internal server error has occurred. Please contact your admin and provide the unique error identifier.")

  fun toProtocolError(): ProtocolError {
    return with(ProtocolError()) {
      id     = this@DiscoveryError.id.toString()
      code   = this@DiscoveryError.code
      title  = this@DiscoveryError.title.toUpperCase()
      detail = this@DiscoveryError.detail
      this
    }
  }
}