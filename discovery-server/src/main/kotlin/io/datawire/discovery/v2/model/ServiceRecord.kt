package io.datawire.discovery.v2.model

import java.io.Serializable


data class ServiceRecord(val key: ServiceKey, val version: String, val properties: Map<String, String>) : Serializable