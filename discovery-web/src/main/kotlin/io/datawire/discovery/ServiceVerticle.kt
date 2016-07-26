package io.datawire.discovery

import io.datawire.discovery.model.DiscoveryInfo
import io.datawire.discovery.model.TenantInfo
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import mdk_discovery.protocol.*
import mdk_protocol.Open


class ServiceVerticle: AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun start() {
    val serverInfo = DiscoveryInfo(config())

    val server = vertx.createHttpServer();
    val router = Router.router(vertx);

    router.get("/ws/v1/:tenant").handler { routingContext ->
      val request = routingContext.request()
      val socket = request.upgrade()

      val tenant = TenantInfo(request.getParam("tenant"))

      openHandshake(socket, tenant)

      socket.handler { buf ->
        try {
          val event = DiscoveryEvent.decode(buf.toString(Charsets.UTF_8))
          event.dispatch(DiscoveryEventHandler(serverInfo, tenant, vertx.eventBus(), socket))
        } catch (ex: Exception) {
          logger.error("Failed to handle received event", ex)
        }
      }

      socket.closeHandler {
        // TODO: Cleanup the client's notification consumer
      }
    }

    server.requestHandler{ router.accept(it) }.listen(config().getInteger("port", 5000));
  }

  private fun openHandshake(socket: ServerWebSocket, tenant: TenantInfo) {
    socket.writeFinalTextFrame(Open().encode())
    socket.writeFinalTextFrame(Clear().encode())

    // TODO: Populate the local servers address
    vertx.eventBus().send<JsonArray>("discovery.LOCAL_ID_HERE.services", JsonObject()) { send ->
      if (send.succeeded()) {
        val activeNodes = send.result().body()
        logger.debug("Retrieval of active nodes succeeded (tenant: {}, active count: {})", tenant, activeNodes.size())

        vertx.executeBlocking<Void>(
            { fut ->
              for (node in activeNodes) {
                socket.writeFinalTextFrame(activeNodes.toString())
              }
            },
            false,
            { res -> })

      } else {
        logger.error("Retrieval of active nodes failed (tenant: {})", tenant, send.cause())
      }
    }
  }
}
