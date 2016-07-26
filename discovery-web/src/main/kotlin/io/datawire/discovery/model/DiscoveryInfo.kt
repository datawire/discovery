package io.datawire.discovery.model

import io.vertx.core.json.JsonObject
import java.util.*


data class DiscoveryInfo(val id: String, val version: String) {

  constructor(config: JsonObject) : this(config.getString("serverId", System.getProperty("discovery.serverId", UUID.randomUUID().toString())), config.getString("version", "1.0"))
}