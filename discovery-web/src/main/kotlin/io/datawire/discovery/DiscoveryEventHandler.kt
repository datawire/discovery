package io.datawire.discovery

import io.datawire.discovery.model.DiscoveryError
import io.datawire.discovery.model.DiscoveryInfo
import io.datawire.discovery.model.TenantInfo
import io.vertx.core.eventbus.EventBus
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import mdk_discovery.protocol.*
import mdk_protocol.Close
import mdk_protocol.Open
import mdk_protocol.ProtocolError


class DiscoveryEventHandler(private val serverInfo: DiscoveryInfo,
                            private val tenant: TenantInfo,
                            private val eventBus: EventBus,
                            private val socket: ServerWebSocket) : DiscoHandler {

  private val logger = LoggerFactory.getLogger(DiscoveryEventHandler::class.java)

  override fun onOpen(event: Open?) {
    event?.let {
      if (it.version == serverInfo.version) {

      } else {
        errorOnProtocolMismatch()
      }
    } ?: errorOnNull()
  }

  override fun onClose(event: Close?) {
    event?.let { closeConnection(Close()) } ?: errorOnNull()
  }

  override fun onClear(event: Clear?) {
    event?.let {
      val error = DiscoveryError.ClientEventNotAllowed()
      closeConnection(error)
    } ?: errorOnNull()
  }

  override fun onActive(event: Active?) {
    event?.let {
      val internalEvent = JsonObject()
      eventBus.publish("discovery.service-registry:active", internalEvent)
    } ?: errorOnNull()
  }

  override fun onExpire(event: Expire?) {
    event?.let {
      val internalEvent = JsonObject()
      eventBus.publish("discovery.service-registry:expire", internalEvent)
    } ?: errorOnNull()
  }

  private fun errorOnProtocolMismatch() {
    val error = DiscoveryError.ProtocolMismatch()
    logger.error("Protocol version mismatch (server: {}, errorId: {}, tenant: {})", serverInfo.id, error.id, tenant)
    closeConnection(error)
  }

  private fun errorOnNull() {
    val error = DiscoveryError.InternalServerError()
    logger.error("Could not process message (server: {}, errorId: {}, tenant: {})", serverInfo.id, error.id, tenant)
    closeConnection(error)
  }

  private fun closeConnection(error: DiscoveryError?) {
    val close = Close()
    if (error != null) {
      logger.error("Danger Will Robinson!")
      close.error = error.toProtocolError()
    }

    closeConnection(close)
  }

  private fun closeConnection(close: Close) {
    socket.writeFinalTextFrame(close.encode())
    socket.close()
  }
}