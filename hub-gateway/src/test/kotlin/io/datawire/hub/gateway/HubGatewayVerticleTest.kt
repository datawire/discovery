package io.datawire.hub.gateway

import io.datawire.hub.gateway.tenant.HubResolverVerticle
import io.datawire.hub.gateway.tenant.SimpleHubResolver
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class HubGatewayVerticleTest {

  @get:Rule
  val timeout = Timeout.seconds(5)

  lateinit var vertx: Vertx
  lateinit var jwt: JWTAuth

  @Before
  fun setup(context: TestContext) {
    //vertx = rule.vertx()
    vertx = Vertx.vertx()

    val configuration = HubGatewayTestSupport().loadConfiguration("valid_HubGatewayConfiguration.yml")

    jwt = configuration.buildJWTAuthProvider(vertx)
    vertx.deployVerticle(HubGatewayVerticle(jwt), context.asyncAssertSuccess())

    vertx.deployVerticle(HubResolverVerticle(configuration.buildHubResolver()), context.asyncAssertSuccess())
  }

  @After
  fun teardown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun sendHttpGetToHealthUrlReturnsHttp200(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()
    http.getNow(8080, "localhost", "/health") { resp ->
      context.assertEquals(200, resp.statusCode())
      async.complete()
    }
  }

  @Test
  fun sendHttpPostWithoutJwtReturnsHttp401(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()
    http.post(8080, "localhost", "/v1/connect") { resp ->
      resp.bodyHandler {
        context.assertEquals(401, resp.statusCode())
        async.complete()
      }
    }.end()
  }

  @Test
  fun sendHttpPostWithJwtContainingKnownTenantReturns200WithHub(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()
    val request = http.post(8080, "localhost", "/v1/connect") { resp ->
      context.assertEquals(200, resp.statusCode())
      context.assertTrue(resp.getHeader("content-type").startsWith("application/json"))

      resp.bodyHandler { body ->
        val json = body.toJsonObject()
        context.assertTrue(json.containsKey("url"))

        val url = json.getString("url")
        val validUrls = setOf("wss://10.0.1.10:52689/v1/messages", "wss://10.0.1.11:52689/v1/messages")

        context.assertTrue(validUrls.contains(url))
        async.complete()
      }
    }

    request.putHeader("Authorization", "Bearer ${generateJwt("datawire")}")
    request.end()
  }

  fun generateJwt(tenant: String): String {
    val jwt = jwt.generateToken(JsonObject(), JWTOptions().addAudience(tenant))
    println(jwt)
    return jwt
  }
}