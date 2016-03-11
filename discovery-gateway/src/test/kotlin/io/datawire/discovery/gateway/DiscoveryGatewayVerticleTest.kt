package io.datawire.discovery.gateway

import io.datawire.discovery.gateway.tenant.DiscoveryResolverVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.Timeout
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class DiscoveryGatewayVerticleTest {

  @get:Rule
  val timeout = Timeout.seconds(5)

  lateinit var vertx: Vertx
  lateinit var jwt: JWTAuth

  @Before
  fun setup(context: TestContext) {
    vertx = Vertx.vertx()

    val configuration = DiscoveryGatewayTestSupport().loadConfiguration("valid_DiscoveryGatewayConfiguration.yml")
    jwt = JWTAuth.create(vertx, configuration.gateway.jsonWebTokenFactory.buildKeyStoreConfig())

    val (gatewayVerticle, gatewayConfig) = configuration.buildGatewayVerticle()

    vertx.deployVerticle(gatewayVerticle, DeploymentOptions().setConfig(gatewayConfig), context.asyncAssertSuccess())
    vertx.deployVerticle(DiscoveryResolverVerticle(configuration.buildDiscoveryResolver()), DeploymentOptions().setWorker(true), context.asyncAssertSuccess())
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
  fun sendHttpPostWithJwtContainingKnownTenantReturns200WithDiscovery(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()
    val request = http.post(8080, "localhost", "/v1/connect") { resp ->
      context.assertEquals(200, resp.statusCode())
      context.assertTrue(resp.getHeader("content-type").startsWith("application/json"))

      resp.bodyHandler { body ->
        val json = body.toJsonObject()
        context.assertTrue(json.containsKey("url"))

        val url = json.getString("url")
        val validUrls = setOf("ws://10.0.1.10:52689/v1/messages", "ws://10.0.1.11:52689/v1/messages")

        context.assertTrue(validUrls.contains(url))
        async.complete()
      }
    }

    request.putHeader("Authorization", "Bearer ${generateJwt("datawire")}")
    request.end()
  }

  fun generateJwt(tenant: String): String {
    val jwt = jwt.generateToken(JsonObject(), JWTOptions().addAudience(tenant))
    return jwt
  }
}