package io.datawire.hub.gateway

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import hub.HubClient
import hub.HubConnectionOptions
import io.datawire.hub.gateway.internal.HubSyncHandler
import io.datawire.quark.netty.QuarkNettyRuntime
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory
import java.util.*


class HubSynchronizerVerticle @JsonCreator constructor(
    @JsonProperty("scheme") private val scheme: String,
    @JsonProperty("host") private val address: String,
    @JsonProperty("port") private val port: Int): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(HubSynchronizerVerticle::class.java)
  private val tenantHubs = HashMap<String, String>()

  override fun start() {
    log.info("starting root hub synchronizer verticle... (root: {0}://{1}:{2})", scheme, address, port)

    val runtime = QuarkNettyRuntime.getRuntime()
    val syncHandler = HubSyncHandler(tenantHubs)
    val options = HubConnectionOptions("", "", "")
    options.setAuthenticationRequired(false)
    options.setHubHost(address)
    options.setHubPort(port)

    val rootHubClient = HubClient(runtime, options)

    //tenantHubs.put("datawire", "bar.d6e.co")
    //tenantHubs.put("pal-corp", "baz.d6e.co")

    vertx.eventBus().localConsumer<String>("hub-lookup") { lookup ->
      val tenantId = lookup.body()
      log.debug("hub lookup request -> (tenant: {0})", tenantId)
      lookup.reply(tenantHubs[tenantId])
    }

    rootHubClient.connect(syncHandler)
  }
}