package io.datawire.discovery.model

import mdk_discovery.Node
import mdk_discovery.protocol.Active
import mdk_discovery.protocol.Expire
import java.io.Serializable
import java.util.*


data class ServiceRecord(val key: ServiceKey, val version: String, val timeToLive: Long, val properties: Map<String, String>) : Serializable {

  val serviceName: String
    get() = key.name

  val tenant: String
    get() = key.tenant

  val address: String
    get() = key.address

  fun toActive(): Active {
    val result = Active()
    result.node = createNode()
    result.ttl  = timeToLive.toDouble()
    return result
  }

  fun toExpire(): Expire {
    val result = Expire()
    result.node = createNode()
    return result
  }

  private fun createNode(): Node {
    val result = Node()
    result.address = address
    result.service = key.name
    result.version = version
    result.properties = HashMap(properties)
    return result
  }
}