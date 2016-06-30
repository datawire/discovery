package io.datawire.discovery

import mdk_discovery.protocol.*
import io.datawire.discovery.model.ServiceKey
import io.datawire.discovery.model.ServiceRecord
import io.datawire.discovery.model.ServiceStore
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.logging.LoggerFactory
import mdk_protocol.Close
import mdk_protocol.Open
import mdk_protocol.ProtocolError
import java.util.*


class DiscoveryMessageHandler(private val tenant         : String,
                              private val messageVersion : String,
                              private val socket         : ServerWebSocket,
                              private val serviceStore   : ServiceStore) : DiscoHandler, Handler<Buffer> {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun handle(buffer: Buffer) {
    try {
      val event = DiscoveryEvent.decode(buffer.toString(Charsets.UTF_8))
      logger.debug("Handling {} event (tenant: {})", event.javaClass.simpleName, tenant)
      when (event) {
        is Active -> onActive(event)
        is Expire -> onExpire(event)
        is Clear  -> onClear(event)
        is Open   -> onOpen(event)
        is Close  -> onClose(event)
        else      -> throw UnsupportedOperationException("TODO: ERROR MESSAGE")
      }
    } catch (th: Throwable) {
      // TODO(plombardi): Send an error message
      logger.error("Error handling client message", th)
      socket.close()
    }
  }

  override fun onOpen(open: Open?) {
    val clientVersion = open?.version
    if (messageVersion != clientVersion) {
      val errorId = UUID.randomUUID().toString()
      logger.error("Protocol version mismatch (tenant: {}, id: {}, server: {}, client: {})",
                   tenant, errorId, messageVersion, clientVersion)

      val close = Close()
      val error = ProtocolError()
      error.code  = "1"
      error.title = "VERSION_MISMATCH"
      error.id    = errorId
      close.error = error
      socket.writeFinalTextFrame(close.encode())
      socket.close()
    }
  }

  override fun onClose(close: Close?) {
    socket.writeFinalTextFrame(Close().encode())
    socket.close()
  }

  override fun onActive(active: Active) {
    val key = ServiceKey(tenant, active.node)
    val record = ServiceRecord(key,
                               active.node.version,
                               active.ttl.toLong(),
                               active.node.properties?.mapValues { it.toString() } ?: hashMapOf<String,String>())


    serviceStore.addRecord(record)
  }

  override fun onExpire(expire: Expire) {
    serviceStore.removeRecord(ServiceKey(tenant, expire.node))
  }

  override fun onClear(reset: Clear?) {
    throw UnsupportedOperationException()
  }
}
