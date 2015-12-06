package io.datawire.hub

import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Datawire Hub service registry unit tests
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


@RunWith(VertxUnitRunner::class)
class ServiceRegistryTest: HubTest("localhost") {
  @Test fun temporaryStub() {
    assertTrue(true)
  }
//
//  @Test fun webSocketAcceptsConnections(context: TestContext) {
//    val async = context.async()
//    webSocket(
//        path = "/v1/services",
//        connectedHandler = { ws ->
//          ws.handler { buf ->
//            context.assertTrue(JsonObject(buf.toString("utf-8")).getString("payload") == "Hello, world!")
//            async.complete()
//          }
//
//          ws.write(Buffer.buffer(
//            JsonObject().put("type", "echo").put("id", 1).put("payload", "Hello, world!").toString()
//          ))
//        },
//        failureHandler = {
//          fail("should not be called!")
//        })
//  }
//
//  @Test fun registryRejectsInvalidPath(context: TestContext) {
//    val async = context.async()
//    webSocket(
//        path = "/this/is/not-a-valid-path",
//        connectedHandler = { ws ->
//          ws.handler { buf ->
//            fail("should not be called!")
//          }
//        },
//        failureHandler = { fail ->
//          context.assertTrue(fail is WebSocketHandshakeException)
//          async.complete()
//        })
//  }
//
//  @Test fun serviceRegistryQueryReturnsRegisteredServices(context: TestContext) {
//    val async = context.async()
//  }
}