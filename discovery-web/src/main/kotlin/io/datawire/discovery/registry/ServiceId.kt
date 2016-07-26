package io.datawire.discovery.registry

import io.vertx.core.json.JsonObject


data class ServiceId(val name: String, val address: String, val tenant: String) {

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        "name" to name,
        "address" to address,
        "tenant" to tenant))
  }
}