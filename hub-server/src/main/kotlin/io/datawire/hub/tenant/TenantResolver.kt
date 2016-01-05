package io.datawire.hub.tenant

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(name="simple", value = SimpleTenantResolver::class),
    JsonSubTypes.Type(name="ec2", value = EC2InstanceTagTenantResolver::class)
)
interface TenantResolver {
  fun resolve(): String
}