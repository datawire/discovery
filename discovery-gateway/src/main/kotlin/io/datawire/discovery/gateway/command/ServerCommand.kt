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

package io.datawire.discovery.gateway.command


import io.datawire.app.Application
import io.datawire.app.Context
import io.datawire.app.command.ContextCommand
import io.datawire.discovery.gateway.DiscoveryGatewayConfiguration
import io.datawire.discovery.gateway.DiscoveryGatewayVerticle
import io.datawire.discovery.gateway.tenant.DiscoveryResolverVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.logging.LoggerFactory
import net.sourceforge.argparse4j.inf.Namespace

class ServerCommand(application: Application<DiscoveryGatewayConfiguration>):
    ContextCommand<DiscoveryGatewayConfiguration>("server", "Run the Discovery Gateway Server", application) {

  private val log = LoggerFactory.getLogger(ServerCommand::class.java)

  override fun run(configuration: DiscoveryGatewayConfiguration?, context: Context?, namespace: Namespace?) {
    log.info("Bootstrapping Discovery Gateway")

    val vertx = configuration!!.buildVertx()
    vertx.deployVerticle(DiscoveryResolverVerticle(configuration.buildDiscoveryResolver()), DeploymentOptions().setWorker(true))

    val (gatewayVerticle, gatewayConfig) = configuration.buildGatewayVerticle()
    vertx.deployVerticle(gatewayVerticle, DeploymentOptions().setConfig(gatewayConfig))

    log.info("Bootstrapped Discovery Gateway")
    System.`in`.read()
  }
}