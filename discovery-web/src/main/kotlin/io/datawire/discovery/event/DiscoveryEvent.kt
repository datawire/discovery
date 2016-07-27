package io.datawire.discovery.event

import io.datawire.discovery.model.ServiceRecord
import io.datawire.discovery.tenant.TenantReference
import io.vertx.core.json.JsonObject
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class DiscoveryEvent(val type: String,
                     val id: UUID = UUID.randomUUID(),
                     val occurrenceTime: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC),
                     val properties: Map<String, Any?> = emptyMap()) {

  constructor(json: JsonObject): this(
      json.getString("type"),
      UUID.fromString(json.getString("id")),
      ZonedDateTime.parse(json.getString("occurrenceTime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      json.getJsonObject("properties").map)

  constructor(type: String, properties: Map<String, Any?> = emptyMap()): this(
      type,
      UUID.randomUUID(),
      ZonedDateTime.now(ZoneOffset.UTC), properties)

  val json: JsonObject
    get() = JsonObject()
      .put("id", id.toString())
      .put("type", type)
      .put("occurrenceTime", occurrenceTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      .put("properties", properties)

  companion object {

    const val NODE_REGISTERED = "service-registered"
    const val NODE_EXPIRED = "service-expired"

    fun serviceRegistered(tenant: TenantReference, service: ServiceRecord): DiscoveryEvent {
      return DiscoveryEvent(NODE_REGISTERED, mapOf(
          "tenant"  to tenant.toJson(),
          "service" to service.toJson()
      ))
    }

    fun serviceExpired(tenant: TenantReference, service: ServiceRecord): DiscoveryEvent {
      return DiscoveryEvent(NODE_EXPIRED, mapOf(
          "tenant"  to tenant.toJson(),
          "service" to service.toJson()
      ))
    }
  }
}