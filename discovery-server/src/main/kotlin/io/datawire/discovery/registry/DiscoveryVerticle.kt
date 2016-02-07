package io.datawire.discovery.registry

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.discovery.auth.QueryJWTAuthHandler
import io.datawire.discovery.registry.model.BaseMessage
import io.datawire.discovery.registry.model.MessageContext
import io.datawire.discovery.tenant.TenantResolver
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.JWTAuthHandler
import java.nio.charset.Charset


abstract class DiscoveryVerticle(
    protected val tenants: TenantResolver,
    protected val registry: ServiceRegistry
): AbstractVerticle() {

  protected lateinit var jwt: JWTAuthHandler
  protected val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
  protected val router: Router = Router.router(vertx)

  override fun start() {
    setup()
    startDiscovery()
  }

  fun setup() {
    val jwtAuth = JWTAuth.create(vertx, config().getJsonObject("jsonWebToken"))
    jwt = QueryJWTAuthHandler(jwtAuth, "/health")

    router.get("/health").handler { rc -> rc.response().setStatusCode(200).end() }
    router.route("/messages/*").handler(jwt)
  }

  abstract fun startDiscovery()

  protected fun processMessage(tenant: String, client: String, buffer: Buffer): Pair<MessageContext, BaseMessage> {
    val message = deserializeMessage(client, buffer)
    return Pair(MessageContext(tenant, client), message)
  }

  protected fun deserializeMessage(client: String, buffer: Buffer): BaseMessage {
    val injections = InjectableValues.Std().addValue("origin", client)
    val reader = objectMapper.readerFor(BaseMessage::class.java).with(injections)
    return reader.readValue<BaseMessage>(buffer.toString(Charsets.UTF_8.name()))
  }

  protected fun serializeMessage(message: BaseMessage, charset: Charset = Charsets.UTF_8): String {
    val writer = objectMapper.writer()
    val json = writer.writeValueAsString(message)
    return Buffer.buffer(json).toString(charset.name())
  }
}