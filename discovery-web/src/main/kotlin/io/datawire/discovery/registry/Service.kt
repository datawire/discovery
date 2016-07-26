package io.datawire.discovery.registry


data class Service(val version: String, val expiry: Long, val properties: Map<String, String>)