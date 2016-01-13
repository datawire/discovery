package io.datawire.hub.gateway.tenant

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.Filter
import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory


class Ec2InstanceTagAddressResolver(private val ec2: AmazonEC2): HubAddressResolver, AbstractVerticle() {

  private val log = LoggerFactory.getLogger(Ec2InstanceTagAddressResolver::class.java)

  override fun resolve(tenant: String): Set<String> {
    val query = createTenantTagQuery(tenant)
    val response = ec2.describeInstances(query)
    return response.reservations[0]?.instances?.map { "ws://hub-${it.instanceId.replace("i-", "")}.datawire.io/" }?.toSet() ?: emptySet()
  }

  private fun createTenantTagQuery(tenant: String): DescribeInstancesRequest {
    return DescribeInstancesRequest().withFilters(Filter("tag:Tenant", listOf(tenant)))
  }

  override fun start() {
    log.info("starting ec2 tag query verticle")

    vertx.eventBus().localConsumer<String>("hub-lookup") { lookup ->
      val tenantId = lookup.body()
      val addresses = resolve(tenantId)

      log.debug("hub lookup request -> (tenant: {0})", tenantId)
      lookup.reply(addresses.firstOrNull() ?: "unknown")
    }
  }
}