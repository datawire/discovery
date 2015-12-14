package io.datawire.hub

import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import org.junit.After
import org.junit.Before
import java.net.ServerSocket


abstract class HubTest(private val bindAddress: String) {

  protected lateinit var vertx: Vertx
  protected var verticlePort: Int = 0

  @Before fun setup(context: TestContext) {
    beforeVerticleDeploy()
    deployVerticle(context)
    afterVerticleDeploy()
  }

  @After fun teardown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  fun beforeVerticleDeploy() = Unit
  fun afterVerticleDeploy() = Unit

  fun deployVerticle(context: TestContext) {
    vertx = Vertx.vertx()
    verticlePort = getRandomPort()

    val deployOptions = DeploymentOptions().setConfig(
        JsonObject().put("port", verticlePort)
    )

    vertx.deployVerticle(ServiceRegistry(), deployOptions, context.asyncAssertSuccess())
  }

  /**
   * Retrieves a random port for use in a test. There is a very small chance the port is not available if another
   * server listens on the acquired port within the tiny amount of time between acquisition and the port being returned
   * and used by the caller.
   */
  fun getRandomPort(): Int {
    val socket = ServerSocket(0)
    val port = socket.localPort
    socket.close()
    return port
  }

  /**
   * configures and starts a WebSocket client
   */
  fun webSocket(
      path: String, connectedHandler: (WebSocket) -> Unit = {}, failureHandler: (Throwable) -> Unit = {}) {

    vertx.createHttpClient().websocket(verticlePort, bindAddress, path, connectedHandler, failureHandler)
  }
}