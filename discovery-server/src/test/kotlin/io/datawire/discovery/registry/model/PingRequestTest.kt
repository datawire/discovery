//package io.datawire.discovery.registry.model
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.kotlin.registerKotlinModule
//import io.datawire.discovery.DiscoveryServiceConfiguration
//import io.datawire.discovery.test.DiscoveryTest
//import io.datawire.util.test.Fixtures
//import io.vertx.core.Vertx
//import io.vertx.core.json.JsonObject
//import io.vertx.ext.auth.jwt.JWTAuth
//import io.vertx.ext.auth.jwt.JWTOptions
//import io.vertx.ext.unit.TestContext
//import io.vertx.ext.unit.junit.RunTestOnContext
//import io.vertx.ext.unit.junit.VertxUnitRunner
//
//import org.assertj.core.api.Assertions.*
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//
//@RunWith(VertxUnitRunner::class)
//class PingRequestTest : DiscoveryTest {
//
//  override  val fixtures = Fixtures()
//
//  private val objectMapper = ObjectMapper().registerKotlinModule()
//
//  lateinit var vertx: Vertx
//  lateinit var jwt: JWTAuth
//
//  @Before
//  fun setup(context: TestContext) {
//    vertx = Vertx.vertx()
//
//    val configuration = buildConfiguration(DiscoveryServiceConfiguration::class.java, "test.yml")
//
//    jwt = configuration.buildJWTAuthProvider(vertx)
//    val tenantResolver = configuration.tenantResolver
//    configuration.deployRegistry(vertx, jwt, tenantResolver.resolve(), context.asyncAssertSuccess())
//  }
//
//  @Test
//  fun deserialize_FromWellFormedJson_ReturnPingRequest() {
//    val json = fixtures.loadFixture("valid_PingRequest.json")
//    val echo = objectMapper.readValue(json, PingRequest::class.java)
//
//    assertThat(echo.id).isNotNull()
//    assertThat(echo.origin).isEqualTo("test")
//  }
//
//  @Test
//  fun sendPingRequest_ReturnPongResponse(context: TestContext) {
//    val async = context.async()
//    val http = vertx.createHttpClient()
//
//    http.websocket(52689, "localhost", "/messages?token=${generateJwt("datawire")}") { it ->
//
//      it.writeFinalTextFrame("""{"type": "ping" }""")
//
//      it.handler { buf ->
//        val json = buf.toJsonObject()
//        context.assertEquals(json.getString("type"), "pong")
//        context.assertEquals(json.getString("origin"), "discovery")
//        async.complete()
//      }
//    }
//  }
//
//  fun generateJwt(tenant: String): String {
//    return jwt.generateToken(JsonObject(), JWTOptions().addAudience(tenant))
//  }
//}