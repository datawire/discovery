package io.datawire.hub.tenant.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize


data class Tenant(
    @JsonProperty("id") val id: TenantId,
    @JsonProperty("name") val name: String,

    @JsonProperty("keys")
    @JsonDeserialize(contentUsing = TenantKeyPair.JacksonDeserializer::class)
    val keys: Map<String, TenantKeyPair>)