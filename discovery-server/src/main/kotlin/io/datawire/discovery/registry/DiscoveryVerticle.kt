package io.datawire.discovery.registry

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.discovery.auth.DiscoveryAuthHandler
import io.datawire.discovery.registry.model.BaseMessage
import io.datawire.discovery.registry.model.MessageContext
import io.datawire.discovery.registry.model.RoutesResponse
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import java.nio.charset.Charset


abstract class DiscoveryVerticle(
    protected val registry: RoutingTable
): AbstractVerticle() {

  protected lateinit var jwt: JWTAuthHandler
  protected val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
  protected val router: Router = Router.router(vertx)

  override fun start() {
    setup()
    start(deploymentID())
  }

  fun setup() {
    val jwtAuth = JWTAuth.create(vertx, config().getJsonObject("jsonWebToken"))
    jwt = DiscoveryAuthHandler(jwtAuth, "/health")

    router.route("/*").handler(CorsHandler.create("*"))
    router.get("/health").handler { rc -> rc.response().setStatusCode(200).end() }
    router.route("/v1/messages/*").handler(jwt)
  }

  abstract fun start(verticleId: String)

  fun publishRoutingTable(tenant: String) {
    val routingTable = registry.mapNamesToEndpoints(tenant)
    vertx.eventBus().publish(
        "routing-table:$tenant:notifications", serializeMessage(RoutesResponse("${deploymentID()}", routingTable)))
  }

  fun sendRoutingTable(tenant: String, socket: ServerWebSocket) {
    val routingTable = registry.mapNamesToEndpoints(tenant)
    socket.writeFinalTextFrame(serializeMessage(RoutesResponse(deploymentID(), routingTable)))
  }

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