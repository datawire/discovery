package io.datawire.hub

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(VertxUnitRunner::class)
class ServiceRegistryTest: HubTest() {

  private lateinit var vertx: Vertx
  private var verticlePort: Int = 0

  @Before fun setup(context: TestContext) {
    vertx = Vertx.vertx()
    verticlePort = getRandomPort()

    val deployOptions = DeploymentOptions().setConfig(
        JsonObject().put("port", verticlePort)
    )

    vertx.deployVerticle(ServiceRegistry(), deployOptions, context.asyncAssertSuccess())
  }

  @Test fun webSocketAcceptsConnections(context: TestContext) {
    val async = context.async()

    vertx.createHttpClient().websocket(verticlePort, "localhost", "/v1/services") { ws ->
      ws.handler { buf ->
        context.assertTrue(JsonObject(buf.toString("utf-8")).getString("payload") == "Hello, world!")
        async.complete()
      }

      ws.write(Buffer.buffer(
          JsonObject().put("type", "echo").put("id", 1).put("payload", "Hello, world!").toString()
      ))
    }
  }

  @Test fun serviceRegistryQueryReturnsRegisteredServices(context: TestContext) {

  }

  @After fun teardown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }
}