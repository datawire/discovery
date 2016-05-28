package io.datawire.discovery.v2.model

import java.io.Serializable


data class ServiceKey(val name: String, val address: String, val tenant: String) : Serializable {
  override fun toString() = "$tenant|$name|$address"
}