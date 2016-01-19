/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.hub.gateway.tenant


import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.Filter
import com.amazonaws.services.ec2.model.InstanceStateName
import com.fasterxml.jackson.annotation.JsonProperty
import io.vertx.core.logging.LoggerFactory


class EC2InstanceHubResolver(private val ec2: AmazonEC2): HubResolver {

  private val log = LoggerFactory.getLogger(EC2InstanceHubResolver::class.java)

  override fun resolve(tenant: String): Set<String> {
    log.info("Resolving Hub addresses for tenant (tenant: {0})", tenant)
    val query = createTenantTagQuery(tenant)
    val instances = ec2.describeInstances(query)

    val addresses = instances.reservations[0]?.instances?.map {
      "wss://hub-${it.instanceId.replace("i-", "")}.datawire.io/" }?.toSet() ?: emptySet()

    log.info("Resolved Hub addresses for tenant (count: {0})", addresses.size)
    return addresses
  }

  private fun createTenantTagQuery(tenant: String): DescribeInstancesRequest {
    return DescribeInstancesRequest()
        .withFilters(Filter("instance-state-name", listOf(InstanceStateName.Running.name)))
        .withFilters(Filter("tag:Role", listOf("dwc:hub")))
        .withFilters(Filter("tag:Tenant", listOf(tenant)))
  }

  data class Factory(@JsonProperty private val region: String): HubResolverFactory {
    override fun build(): HubResolver {
      val ec2 = AmazonEC2Client().withRegion<AmazonEC2Client>(Regions.fromName(region))
      return EC2InstanceHubResolver(ec2)
    }
  }
}