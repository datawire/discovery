package io.datawire.discovery.gateway.tenant

import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.ReplyException
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
class DiscoveryResolverVerticleTest {

  @get:Rule
  val timeout = Timeout.seconds(5)

  lateinit var vertx: Vertx
  lateinit var eventBus: EventBus

  @Before
  fun setup(context: TestContext) {
    vertx = Vertx.vertx()
    eventBus = vertx.eventBus()

    val resolver = SimpleDiscoveryServerResolver(setOf("10.0.1.10:52689", "10.0.1.11:52689", "10.0.2.10:52689"))

    vertx.deployVerticle(DiscoveryResolverVerticle(resolver), context.asyncAssertSuccess())
  }

  @After
  fun teardown(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun resolverSuccessfullyReturnsDiscoveryServersForExistentTenant(context: TestContext) {
    val async = context.async()
    eventBus.send<String>("discovery-resolver", "datawire") { it ->
      context.assertTrue(it.succeeded())
      context.assertTrue(setOf("10.0.1.10:52689", "10.0.1.11:52689", "10.0.2.10:52689").contains(it.result().body()))
      async.complete()
    }
  }
}