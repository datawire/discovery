package io.datawire.hub.gateway.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hub.DefaultHubHandler
import hub.message.Synchronize
import io.datawire.hub.gateway.jackson.QuickAndDirtyServicesMapper


class HubSyncHandler(val tenantHubs: MutableMap<String, String>): DefaultHubHandler() {

  private val mapper = QuickAndDirtyServicesMapper(jacksonObjectMapper())

  override fun onSynchronize(sync: Synchronize?) {
    val map = mapper.toMap(sync!!.data)

    // bad code that works for now.
    //
    // services contains a Map { service name, [endpoints] }. In the Root Hub there is only ever 1 endpoint AND the
    // service name is the tenant ID
    val hubs = map["services"]
    if (hubs is Map<*, *>) {
      for (tenantHub in hubs) {
        val hubInfo = (tenantHub.value as List<*>)[0]
        if (hubInfo is Map<*, *>) {
          val addrInfo = hubInfo["address"] as Map<*, *>
          val host = addrInfo["host"]

          val portInfo = hubInfo["port"] as Map<*, *>
          val port = portInfo["port"] as Int

          tenantHubs.put(tenantHub.key as String, "ws://$host:$port/")
        }
      }
    }
  }
}