package io.datawire.discovery.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.datawire.discovery.auth.JWTAuthProviderFactory
import io.datawire.discovery.cluster.HazelcastConfiguration
import io.datawire.discovery.registry.*
import io.datawire.discovery.tenant.TenantResolver
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


@JsonTypeInfo(
    include   = JsonTypeInfo.As.PROPERTY,
    property  = "type",
    use       = JsonTypeInfo.Id.NAME
)
@JsonSubTypes(
    JsonSubTypes.Type(name="standalone", value = StandaloneDiscoveryConfiguration::class),
    JsonSubTypes.Type(name="shared-hazelcast", value = SharedHazelcastDiscoveryConfiguration::class)
)
abstract class DiscoveryConfiguration(
    val bindAddress: String,
    val port: Int,
    val tenants: TenantResolver,
    val jsonWebTokenFactory: JWTAuthProviderFactory
) {
  abstract fun buildDiscoveryVerticle(registry: ServiceRegistry): Pair<DiscoveryVerticle, JsonObject>

  protected fun buildVerticleConfig(): JsonObject {
    return JsonObject()
        .put("bindAddress", bindAddress)
        .put("port", port)
        .put("jsonWebToken", jsonWebTokenFactory.buildKeyStoreConfig())
  }
}

class StandaloneDiscoveryConfiguration @JsonCreator constructor(
    @JsonProperty("bindAddress") bindAddress: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("tenants") tenants: TenantResolver,
    @JsonProperty("jsonWebToken") jsonWebToken: JWTAuthProviderFactory
): DiscoveryConfiguration(bindAddress, port, tenants, jsonWebToken) {

  override fun buildDiscoveryVerticle(registry: ServiceRegistry): Pair<DiscoveryVerticle, JsonObject> {
    return Pair(SharedServiceRegistryVerticle(tenants, registry), buildVerticleConfig())
  }
}

class SharedHazelcastDiscoveryConfiguration @JsonCreator constructor(
    @JsonProperty("bindAddress") bindAddress: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("tenants") tenants: TenantResolver,
    @JsonProperty("jsonWebToken") jsonWebToken: JWTAuthProviderFactory
): DiscoveryConfiguration(bindAddress, port, tenants, jsonWebToken) {

  override fun buildDiscoveryVerticle(registry: ServiceRegistry): Pair<DiscoveryVerticle, JsonObject> {
    return Pair(SharedServiceRegistryVerticle(tenants, registry), buildVerticleConfig())
  }
}