package io.datawire.hub.registry

import io.datawire.hub.HubConfiguration
import io.datawire.hub.test.HubTest
import io.datawire.util.test.Fixtures
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocketStream
import io.vertx.core.http.WebsocketVersion
import io.vertx.core.http.impl.HttpClientImpl
import io.vertx.core.impl.VertxInternal
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.assertj.core.api.Assertions.*

@RunWith(VertxUnitRunner::class)
class LocalServiceRegistryVerticleTest : HubTest {

  override val fixtures = Fixtures()

  @get:Rule
  public val rule = RunTestOnContext()

  lateinit var vertx: Vertx
  lateinit var jwt: JWTAuth

  @Before
  fun setup(context: TestContext) {
    vertx = rule.vertx()

    val configuration = buildConfiguration(HubConfiguration::class.java, "test.yml")

    jwt = configuration.buildJWTAuthProvider(vertx)
    val tenantResolver = configuration.tenantResolver
    configuration.deployRegistry(vertx, jwt, tenantResolver.resolve(), context.asyncAssertSuccess())
  }

  @Test
  fun sendHttpGetToHealthUrlReturnsHttp200(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()
    http.getNow(52689, "localhost", "/health") { resp ->
      context.assertEquals(200, resp.statusCode())
      async.complete()
    }
  }

  @Test
  fun sendHttpPostWithoutJwtReturnsHttp401(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()
    http.post(52689, "localhost", "/messages/foobar") { resp ->
      context.assertEquals(401, resp.statusCode())
      async.complete()
    }.end()
  }

  @Test
  fun connectToHubWithJWTInQueryParams(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->
      it.close()
      async.complete()
    }
  }

  @Test
  fun connectToHubWithJWTInQueryParamsButJWTContainsIncorrectAudience(context: TestContext) {
    val async = context.async()

    // Vert.x Core Bug: https://github.com/vert-x3/vertx-web/issues/133
    //
    // Vert.x core client does not pass the exception properly.
    //
    val issue133Handler = { cause: Throwable ->
      context.assertEquals(WebSocketHandshakeException::class.java, cause.javaClass)
      async.complete()
    }

    val http = object : HttpClientImpl(vertx as VertxInternal, HttpClientOptions()) {
      override fun websocketStream(port: Int, host: String?, requestURI: String?, headers: MultiMap?, version: WebsocketVersion?): WebSocketStream? {
        return websocketStream(port, host, requestURI, headers, version, null).exceptionHandler(issue133Handler)
      }
    }

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("foobar")}") {}
  }

  fun generateJwt(tenant: String): String {
    return jwt.generateToken(JsonObject(), JWTOptions().addAudience(tenant))
  }
}