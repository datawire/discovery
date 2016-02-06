package io.datawire.discovery.command

import com.hazelcast.config.Config
import io.datawire.app.Application
import io.datawire.app.Context
import io.datawire.app.command.ContextCommand
import io.datawire.discovery.DiscoveryConfiguration
import io.datawire.discovery.registry.DistributedServiceRegistry
import io.datawire.discovery.registry.SharedServiceRegistryVerticle
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.logging.LoggerFactory
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import net.sourceforge.argparse4j.inf.Namespace


class SharedServerCommand(application: Application<DiscoveryConfiguration>):
    ContextCommand<DiscoveryConfiguration>("shared-server", "Run the Discovery server", application) {

  private val log = LoggerFactory.getLogger(ServerCommand::class.java)

  override fun run(configuration: DiscoveryConfiguration?, context: Context?, namespace: Namespace?) {
    log.info("Bootstrapping Discovery")

    val hzConfig = Config()
    val cluster = HazelcastClusterManager(hzConfig)
    Vertx.clusteredVertx(VertxOptions().setClusterManager(cluster)) {
      if (it.succeeded()) {
        it.result().deployVerticle(SharedServiceRegistryVerticle(DistributedServiceRegistry(cluster.hazelcastInstance)))
      }
    }
  }
}