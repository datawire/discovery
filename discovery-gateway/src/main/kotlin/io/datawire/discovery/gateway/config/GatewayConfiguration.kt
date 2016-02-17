package io.datawire.discovery.gateway.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.datawire.discovery.auth.JWTAuthProviderFactory
import io.datawire.discovery.gateway.DiscoveryGatewayVerticle
import io.vertx.core.json.JsonObject


@JsonTypeInfo(
    include   = JsonTypeInfo.As.PROPERTY,
    property  = "type",
    use       = JsonTypeInfo.Id.NAME
)
@JsonSubTypes(
    JsonSubTypes.Type(name="simple", value = StandaloneGatewayConfiguration::class)
)
abstract class GatewayConfiguration(
    val bindAddress: String,
    val port: Int,
    val jsonWebTokenFactory: JWTAuthProviderFactory
) {
  abstract fun buildGatewayVerticle(): Pair<DiscoveryGatewayVerticle, JsonObject>

  protected fun buildVerticleConfig(): JsonObject {
    return JsonObject()
        .put("bindAddress", bindAddress)
        .put("port", port)
        .put("jsonWebToken", jsonWebTokenFactory.buildKeyStoreConfig())
  }
}

class StandaloneGatewayConfiguration @JsonCreator constructor(
    @JsonProperty("bindAddress") bindAddress: String,
    @JsonProperty("port") port: Int,
    @JsonProperty("jsonWebToken") jsonWebToken: JWTAuthProviderFactory
): GatewayConfiguration(bindAddress, port, jsonWebToken) {

  override fun buildGatewayVerticle(): Pair<DiscoveryGatewayVerticle, JsonObject> {
    return Pair(DiscoveryGatewayVerticle(), buildVerticleConfig())
  }
}