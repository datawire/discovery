package io.datawire.discovery.v2.model

import discovery.Node
import java.io.Serializable


data class ServiceKey(val name: String, val address: String, val tenant: String) : Serializable {

  constructor(tenant: String, node: Node): this(node.service, node.address, tenant)

  override fun toString() = "$tenant|$name|$address"
}