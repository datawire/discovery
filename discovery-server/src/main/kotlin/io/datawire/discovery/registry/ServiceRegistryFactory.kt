package io.datawire.discovery.registry

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(name="simple", value = SimpleServiceRegistryFactory::class)
)
interface ServiceRegistryFactory {
  fun build(vertx: Vertx, jwt: JWTAuth, tenant: String): ServiceRegistryVerticle

  fun deploy(vertx: Vertx, jwt: JWTAuth, tenant: String, onCompletion: Handler<AsyncResult<String>>)
}