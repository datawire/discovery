package io.datawire.discovery.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.hazelcast.core.HazelcastInstance
import io.datawire.discovery.auth.JWTAuthProviderFactory
import io.datawire.discovery.registry.*
import io.datawire.discovery.tenant.TenantResolver
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
  abstract fun buildDiscoveryVerticle(registry: RoutingTable): Pair<DiscoveryVerticle, JsonObject>

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

  override fun buildDiscoveryVerticle(registry: RoutingTable): Pair<DiscoveryVerticle, JsonObject> {
    return Pair(PrototypeServiceRegistryVerticle(tenants, registry), buildVerticleConfig())
  }
}

class SharedHazelcastDiscoveryConfiguration @JsonCreator constructor(
    @JsonProperty("bindAddress") bindAddress: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("tenants") tenants: TenantResolver,
    @JsonProperty("jsonWebToken") jsonWebToken: JWTAuthProviderFactory,
    @JsonProperty("mode") val mode: String
): DiscoveryConfiguration(bindAddress, port, tenants, jsonWebToken) {

  override fun buildDiscoveryVerticle(registry: RoutingTable): Pair<DiscoveryVerticle, JsonObject> {
    return Pair(PrototypeServiceRegistryVerticle(tenants, registry), buildVerticleConfig())
  }

  fun buildDiscoveryVerticle(hazelcast: HazelcastInstance): Pair<DiscoveryVerticle, JsonObject> {
    val registry = when(mode) {
      "replicated"  -> ReplicatedRoutingTable(hazelcast)
      "partitioned" -> PartitionedRoutingTable(hazelcast)
      else -> {
        throw IllegalArgumentException("Unknown registry mode (mode: $mode)")
      }
    }

    return Pair(SharedDiscoveryVerticle(tenants, registry, hazelcast), buildVerticleConfig())
  }
}