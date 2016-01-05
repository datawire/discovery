package io.datawire.hub.tenant

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.util.EC2MetadataUtils
import com.fasterxml.jackson.annotation.JsonCreator
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.TimeUnit


class EC2InstanceTagTenantResolver(private val ec2: AmazonEC2): TenantResolver {

  private val log = LoggerFactory.getLogger(EC2InstanceTagTenantResolver::class.java)

  @JsonCreator constructor(): this(AmazonEC2Client(DefaultAWSCredentialsProviderChain()))

  override fun resolve(): String {
    val instanceId = EC2MetadataUtils.getInstanceId()
    val request = DescribeInstancesRequest().withInstanceIds(instanceId)
    while(true) {
      log.info("Polling for tenant information in EC2 (instance: {0}, tag: {1})", instanceId, "Tenant")
      val response = ec2.describeInstances(request)
      val instance = response.reservations[0].instances[0]
      val tenantTag = instance.tags.find {
        it.key.equals("Tenant")
            && it.value != null
            && it.value.isNotBlank()
            && !it.value.equals("unassigned", ignoreCase = true) }

      if (tenantTag != null) {
        return tenantTag.value
      } else {
        log.info("Failed to find tenant information in EC2; Waiting...")
        TimeUnit.SECONDS.sleep(3)
      }
    }
  }
}