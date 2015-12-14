package io.datawire.hub.tenant

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(name="simple", value = SimpleTenantStoreFactory::class)
)
interface TenantStoreFactory {
  fun build(): TenantStore
}

