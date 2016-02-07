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
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.logging.LoggerFactory
import net.sourceforge.argparse4j.inf.Namespace

import io.datawire.discovery.config.ClusterManagers.*
import io.datawire.discovery.monitoring.MetricsVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.ext.dropwizard.DropwizardMetricsOptions


class ServerCommand(application: Application<DiscoveryServiceConfiguration>):
    ContextCommand<DiscoveryServiceConfiguration>("server", "Run the Discovery server", application) {

  private val log = LoggerFactory.getLogger(ServerCommand::class.java)

  override fun run(configuration: DiscoveryServiceConfiguration?, context: Context?, namespace: Namespace?) {
    configuration!!

    log.info("Bootstrapping Discovery")

    when(configuration.clusterManager) {
      is Hazelcast  -> deployHazelcastClustered(configuration)
      is Standalone -> deployStandalone(configuration)
    }

    System.`in`.read()
  }

  fun deployHazelcastClustered(config: DiscoveryServiceConfiguration) {
    val clusterManager = (config.clusterManager as ClusterManagers.Hazelcast).buildClusterManager()

    val vertxOptions = VertxOptions()
        .setClusterManager(clusterManager)
        .setMetricsOptions(DropwizardMetricsOptions().setEnabled(true))

    Vertx.clusteredVertx(vertxOptions) {
      if (it.succeeded()) {
        val vertx = it.result()
        val (verticle, verticleConfig) = config.buildSharedDiscoveryVerticle(clusterManager.hazelcastInstance)
        vertx.deployVerticle(verticle, DeploymentOptions().setConfig(verticleConfig))
        vertx.deployVerticle(MetricsVerticle())
      }
    }
  }

  fun deployStandalone(config: DiscoveryServiceConfiguration) {
    Vertx.vertx()
  }
}