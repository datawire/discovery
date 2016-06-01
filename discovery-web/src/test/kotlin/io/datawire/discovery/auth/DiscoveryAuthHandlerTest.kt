package io.datawire.discovery.auth

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.Router
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.util.*

/**
 * Unit tests for [DiscoveryAuthHandler]. The DiscoveryAuthHandler only checks to see whether the provided
 * Json Web Token (JWT) can be verified (signature match). It is not concerned with verifying any claims provided by
 * the token.
 *
 * TODO: Enhance these test cases once more stringent claim validation is implemented.
 */


@RunWith(VertxUnitRunner::class)
class DiscoveryAuthHandlerTest : io.datawire.discovery.DiscoveryTest() {

  lateinit private var validJsonWebToken: String
  lateinit private var invalidJsonWebToken: String
  lateinit private var authProvider: JWTAuth

  lateinit private var vertx: Vertx
  lateinit private var socket: ServerSocket

  private var port: Int = 0

  @Before
  fun setup(context: TestContext) {
    vertx = Vertx.vertx()

    socket = ServerSocket(0)
    port = socket.localPort
    socket.close()

    val jsonWebTokenKeyStoreConfig = buildJsonWebTokenKeyStoreConfig(
        path = resourcePath("ks-hmac256-notasecret.jceks"),
        password = "notasecret")

    vertx.deployVerticle(
        TestVerticle(),
        DeploymentOptions().setConfig(JsonObject(mapOf(
            "http.port" to port,
            "auth.jwt"  to jsonWebTokenKeyStoreConfig)
        )),
        context.asyncAssertSuccess())

    authProvider = JWTAuth.create(vertx, jsonWebTokenKeyStoreConfig)

    validJsonWebToken = authProvider.generateToken(JsonObject().put("aud", "VALID_AUDIENCE"), JWTOptions())
    invalidJsonWebToken = makeInvalidToken("INVALID_AUDIENCE")
  }

  @After
  fun teardown(context: TestContext) {
    socket.close()
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun useTokenQueryParameterIfPresent(context: TestContext) {
    val async = context.async()

    val http = vertx.createHttpClient()
    val request = http.post(port, "localhost", "/authenticated?token=$validJsonWebToken") { resp ->
      resp.bodyHandler {
        context.assertEquals(204, resp.statusCode())

        http.close()
        async.complete()
      }
    }

    request.end()
  }

  @Test
  fun useAuthorizationHeaderWithBearerTokenIfPresent(context: TestContext) {
    val async = context.async()

    val http = vertx.createHttpClient()
    val request = http.post(port, "localhost", "/authenticated") { resp ->
      resp.bodyHandler {
        context.assertEquals(204, resp.statusCode())

        http.close()
        async.complete()
      }
    }

    request.putHeader("Authorization", "Bearer $validJsonWebToken")
    request.end()
  }

  @Test
  fun useTokenQueryParameterBeforeAttemptingToUseAuthorizationHeaderWithBearerToken(context: TestContext) {
    val async = context.async()

    val http = vertx.createHttpClient()

    val request = http.post(port, "localhost", "/authenticated?token=$validJsonWebToken") { resp ->
      resp.bodyHandler {
        context.assertEquals(204, resp.statusCode())

        http.close()
        async.complete()
      }
    }

    // If the logic in the handler is not correct then the invalid token will be used.
    request.putHeader("Authorization", "Bearer $invalidJsonWebToken")
    request.end()
  }

  @Test
  fun failIfTokenIsIncorrectlySigned(context: TestContext) {
    val async = context.async()

    val http = vertx.createHttpClient()

    val request = http.post(port, "localhost", "/authenticated?token=$invalidJsonWebToken") { resp ->
      resp.bodyHandler {
        context.assertEquals(401, resp.statusCode())

        http.close()
        async.complete()
      }
    }

    request.end()
  }

  private fun makeInvalidToken(aud: String): String {
    val (header, claims, signature) = validJsonWebToken.split(".")
    val decodedClaims = Base64.getUrlDecoder().decode(claims)

    val typeFactory = objectMapper.typeFactory
    val mapType = typeFactory.constructMapType(HashMap::class.java, String::class.java, String::class.java)
    val claimsMap = objectMapper.readValue<HashMap<String, String>>(decodedClaims, mapType)
    claimsMap["aud"] = aud
    val falseClaims = objectMapper.writeValueAsBytes(claimsMap)

    return listOf(header, Base64.getUrlEncoder().encodeToString(falseClaims), signature).joinToString(".")
  }

  class TestVerticle : AbstractVerticle() {
    override fun start() {
      val router = Router.router(this.vertx)
      val authProvider = JWTAuth.create(this.vertx, config().getJsonObject("auth.jwt"))

      router.post("/authenticated").handler(DiscoveryAuthHandler(authProvider, null))
      router.post("/authenticated").handler { rc ->
        rc.response().setStatusCode(204).end()
      }

      val server = this.vertx.createHttpServer()
      server.requestHandler { router.accept(it) }.listen(config().getInteger("http.port"))
    }
  }
}