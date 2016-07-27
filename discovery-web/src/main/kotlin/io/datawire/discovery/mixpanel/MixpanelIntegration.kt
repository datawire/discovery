package io.datawire.discovery.mixpanel

import com.mixpanel.mixpanelapi.ClientDelivery
import com.mixpanel.mixpanelapi.MessageBuilder
import com.mixpanel.mixpanelapi.MixpanelAPI
import io.datawire.discovery.Discovery
import io.datawire.discovery.event.DiscoveryEvent
import io.datawire.discovery.tenant.TenantReference
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import org.json.JSONObject

import java.util.concurrent.TimeUnit.*

class MixpanelIntegration(): AbstractVerticle() {

  companion object {
    const val CONFIG_KEY = "mixpanelIntegration"
  }

  private val logger = LoggerFactory.getLogger(MixpanelIntegration::class.java)

  private val mixpanel = MixpanelAPI()

  var undelivered = ClientDelivery()

  override fun start() {
    vertx.eventBus().localConsumer<JsonObject>(config().getString("eventBusAddress")) { msg ->
      msg.body()?.let { event ->
        when(event.getString("type")) {
          DiscoveryEvent.NODE_REGISTERED -> addNodeRegisteredEvent(event)
          DiscoveryEvent.NODE_EXPIRED    -> addNodeExpiredEvent(event)
        }
      }
    }

    vertx.setPeriodic(SECONDS.toMillis(config().getLong("publishFrequency", 300))) {
      logger.info("Preparing to publish undelivered messages to Mixpanel")

      // Perform the swap out here rather than in the executeBlocking call because I'm not convinced Mixpanel's code
      // is thread-safe and that matters because an executeBlocking call occurs on a separate execution thread.
      val pending = undelivered
      undelivered = ClientDelivery()

      vertx.executeBlocking<Void>(
          { fut ->
            mixpanel.deliver(pending)
            fut.complete()
          },
          { res ->
            if (res.succeeded()) {
              logger.info("Publish undelivered messages to Mixpanel succeeded")
            } else {
              logger.error("Publish undelivered messages to Mixpanel failed", res.cause())
            }
          })
    }
  }

  private fun addNodeRegisteredEvent(json: JsonObject) {
    val tenantReference = TenantReference(json.getJsonObject("properties").getJsonObject("tenant"))
    val serviceInfo = json.getJsonObject("properties").getJsonObject("service")

    val eventProperties = JSONObject(mapOf(
        "orgId"   to tenantReference.id,
        "email"   to tenantReference.user,
        "nodeId"  to serviceInfo.getJsonObject("properties", JsonObject()).getString("datawire_nodeId", "none"),
        "service" to serviceInfo.getString("name"),
        "version" to serviceInfo.getString("version")
    ))

    val message = MessageBuilder(System.getProperty("app.mixpanel-token", config().getString("token")))
        .event(tenantReference.id, "NodeRegistered", eventProperties)

    undelivered.addMessage(message)
  }

  private fun addNodeExpiredEvent(json: JsonObject) {
    val tenantReference = TenantReference(json.getJsonObject("properties").getJsonObject("tenant"))
    val serviceInfo = json.getJsonObject("properties").getJsonObject("service")

    val eventProperties = JSONObject(mapOf(
        "orgId"   to tenantReference.id,
        "email"   to tenantReference.user,
        "nodeId"  to serviceInfo.getJsonObject("properties", JsonObject()).getString("datawire_nodeId", "none"),
        "service" to serviceInfo.getString("name"),
        "version" to serviceInfo.getString("version")
    ))

    val message = MessageBuilder(System.getProperty("app.mixpanel-token", config().getString("token")))
        .event(tenantReference.id, "NodeExpired", eventProperties)

    undelivered.addMessage(message)
  }
}