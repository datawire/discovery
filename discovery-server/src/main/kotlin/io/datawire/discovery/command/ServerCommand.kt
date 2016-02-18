/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.discovery.command

import io.datawire.app.Application
import io.datawire.app.Context
import io.datawire.app.command.ContextCommand
import io.datawire.discovery.DiscoveryServiceConfiguration
import io.datawire.discovery.config.ClusterManagers
import io.datawire.discovery.config.ClusterManagers.Hazelcast
import io.datawire.discovery.config.ClusterManagers.Standalone
import io.datawire.discovery.monitoring.MetricsVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.ext.dropwizard.DropwizardMetricsOptions
import net.sourceforge.argparse4j.inf.Namespace
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.TimeUnit


class ServerCommand(application: Application<DiscoveryServiceConfiguration>):
    ContextCommand<DiscoveryServiceConfiguration>("server", "Run the Discovery server", application) {

  private val log = LoggerFactory.getLogger(ServerCommand::class.java)

  fun deployHazelcastClustered(config: DiscoveryServiceConfiguration) {


    val clusterManager = (config.clusterManager as ClusterManagers.Hazelcast).buildClusterManager()

    val vertxOptions = VertxOptions()
        .setClusterManager(clusterManager)
        .setMetricsOptions(DropwizardMetricsOptions().setEnabled(true))

    Vertx.clusteredVertx(vertxOptions) {
      if (it.succeeded()) {
        val serverId = config.serverId
        val vertx = it.result()
        val hazelcast = clusterManager.hazelcastInstance
//        val discoveryServers = hazelcast.getMap<String, Long>("discovery-servers")

//        discoveryServers.putAsync(serverId, Instant.now().toEpochMilli(), 10, TimeUnit.SECONDS)
//        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(5)) {
//          log.debug("Sending heartbeat for server: {}", serverId)
//          discoveryServers.containsKey(serverId)
//        }

        val (verticle, verticleConfig) = config.buildSharedDiscoveryVerticle(hazelcast)
        vertx.deployVerticle(verticle, DeploymentOptions().setConfig(verticleConfig))
        vertx.deployVerticle(MetricsVerticle())
      }
    }
  }

  fun deployStandalone(config: DiscoveryServiceConfiguration) {
    Vertx.vertx()
  }

  override fun run(configuration: DiscoveryServiceConfiguration?, context: Context?, namespace: Namespace?) {
    runServer(configuration!!, context!!, namespace!!)
  }

  fun runServer(configuration: DiscoveryServiceConfiguration, context: Context, namespace: Namespace) {
    when(configuration.clusterManager) {
      is Hazelcast  -> {
        deployHazelcastClustered(configuration)
      }
      is Standalone -> deployStandalone(configuration)
    }

    System.`in`.read()
  }

  private fun bootstrap(config: DiscoveryServiceConfiguration) {
    val serverId = config.serverId
    log.info("Bootstrapping Discovery Server... (server-id: {})", serverId)



    log.debug("Bootstrapped Discovery Server...")
  }
}