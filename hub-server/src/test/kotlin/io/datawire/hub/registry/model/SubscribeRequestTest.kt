package io.datawire.hub.registry.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.HubConfiguration
import io.datawire.hub.test.HubTest
import io.datawire.util.test.Fixtures
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI


@RunWith(VertxUnitRunner::class)
class SubscribeRequestTest : HubTest {
  override  val fixtures = Fixtures()

  private val objectMapper = ObjectMapper().registerKotlinModule()

  @get:Rule
  val rule = RunTestOnContext()

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
  fun deserialize_FromWellFormedJson_returnSubscribeNotification() {
    val json = fixtures.loadFixture("valid_SubscribeNotification.json")
    val echo = objectMapper.readValue(json, SubscribeNotification::class.java)

    Assertions.assertThat(echo.id).isNotNull()
    Assertions.assertThat(echo.origin).isEqualTo("test")
  }

  @Test
  fun sendSubscribeNotification_emptyRegistry_returnEmptyMap(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->

      it.writeFinalTextFrame("""{"type": "subscribe"}""")

      it.handler { buf ->
        val json = buf.toJsonObject()
        context.assertEquals(json.getString("type"), "routes")
        context.assertEquals(json.getJsonObject("services"), JsonObject())
        async.complete()
      }
    }
  }

  @Test
  fun sendSubscribeNotification_nonEmptyRegistry_returnMappedEndpoints(context: TestContext) {
    val async = context.async()
    val http = vertx.createHttpClient()

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->
      it.writeFinalTextFrame(generateRegisterMessage("foo-service", URI.create("https://10.0.1.10:443/")))
    }

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->
      it.writeFinalTextFrame(generateRegisterMessage("foo-service", URI.create("https://10.0.1.11:443/")))
    }

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->
      it.writeFinalTextFrame(generateRegisterMessage("bar-service", URI.create("http://10.0.2.20:8080/")))
    }

    Thread.sleep(3000)

    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->

      it.writeFinalTextFrame("""{"type": "subscribe"}""")

      it.handler { buf ->
        val json = buf.toJsonObject()
        context.assertEquals(json.getString("type"), "routes")
        context.assertEquals(JsonObject(mapOf(
            "bar-service" to listOf(
                JsonObject(mapOf(
                    "scheme"  to "http",
                    "host"    to "10.0.2.20",
                    "port"    to 8080,
                    "uri"     to "http://10.0.2.20:8080"
                ))
            ),
            "foo-service" to JsonArray().add(JsonObject(mapOf(
                "scheme"  to "https",
                "host"    to "10.0.1.10",
                "port"    to 443,
                "uri"     to "https://10.0.1.10:443"
            ))).add(JsonObject(mapOf(
                "scheme"  to "https",
                "host"    to "10.0.1.11",
                "port"    to 443,
                "uri"     to "https://10.0.1.11:443"
            )))
        )), json.getJsonObject("services"))
        it.close()
        async.complete()
      }
    }
  }

  fun generateJwt(tenant: String): String {
    return jwt.generateToken(JsonObject(), JWTOptions().addAudience(tenant))
  }

  fun toJson(message: BaseMessage): String {
    return objectMapper.writeValueAsString(message)
  }

  fun generateRegisterMessage(name: String, uri: URI): String {
    val msg = """{
    "type": "register",
    "name": "$name",
    "endpoint": {
      "scheme": "${uri.scheme}",
      "host": "${uri.host}",
      "port": ${uri.port}
    }
    }"""

    return msg
  }
}