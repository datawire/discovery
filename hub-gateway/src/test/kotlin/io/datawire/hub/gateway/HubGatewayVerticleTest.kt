package io.datawire.hub.gateway

import io.datawire.hub.gateway.tenant.HubResolverVerticle
import io.datawire.hub.gateway.tenant.SimpleHubResolver
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class HubGatewayVerticleTest {

  @get:Rule
  public val rule = RunTestOnContext()

  lateinit var vertx: Vertx

  @Before
  fun setup(context: TestContext) {
    vertx = rule.vertx()

    val configuration = HubGatewayTestSupport().loadConfiguration("valid_HubGatewayConfiguration.yml")

    val jwt = configuration.buildJWTAuthProvider(vertx)
    vertx.deployVerticle(HubGatewayVerticle(jwt))

    vertx.deployVerticle(HubResolverVerticle(configuration.buildHubResolver()), context.asyncAssertSuccess())
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
      context.assertEquals(401, resp.statusCode())
      async.complete()
    }.end()
  }
}