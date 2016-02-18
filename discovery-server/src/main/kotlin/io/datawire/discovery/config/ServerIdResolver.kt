package io.datawire.discovery.config

import com.amazonaws.util.EC2MetadataUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    include   = JsonTypeInfo.As.PROPERTY,
    property  = "type",
    use       = JsonTypeInfo.Id.NAME
)
@JsonSubTypes(
    JsonSubTypes.Type(name="static",          value = ServerIdResolver.StaticServerIdResolver::class),
    JsonSubTypes.Type(name="ec2-instance-id", value = ServerIdResolver.Ec2InstanceIdResolver::class)
)
sealed class ServerIdResolver {

  abstract fun resolve(): String

  class StaticServerIdResolver @JsonCreator constructor(@JsonProperty("id") private val id: String) : ServerIdResolver() {
    override fun resolve() = id
  }

  class Ec2InstanceIdResolver @JsonCreator constructor() : ServerIdResolver() {
    override fun resolve(): String {
      return EC2MetadataUtils.getInstanceId()
    }
  }
}