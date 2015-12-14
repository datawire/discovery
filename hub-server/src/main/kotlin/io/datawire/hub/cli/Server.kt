package io.datawire.hub.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.ServiceRegistry
import io.datawire.hub.config.Configuration
import io.datawire.hub.service.LocalServiceRegistryVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import net.sourceforge.argparse4j.inf.Namespace
import java.io.File

/**
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 * @since 1.0
 */


class Server(private val configuration: Configuration): Runnable {

  private val vertx = Vertx.vertx()

  override fun run() {
    val tenantStore = configuration.tenantStore.build()
    val bundle = configuration.serviceRegistry.build(tenantStore)

    vertx.deployVerticle(bundle.verticle, bundle.options)
    System.`in`.read()
  }

  companion object Factory {

    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun build(namespace: Namespace): Server {
      val configFile = namespace.get<File>("config")
      val config = mapper.readValue(configFile, Configuration::class.java)
      return Server(config)
    }
  }
}