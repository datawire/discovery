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
import io.datawire.discovery.DiscoveryConfiguration
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import net.sourceforge.argparse4j.inf.Namespace


class ServerCommand(application: Application<DiscoveryConfiguration>):
    ContextCommand<DiscoveryConfiguration>("server", "Run the Discovery server", application) {

  private val log = LoggerFactory.getLogger(ServerCommand::class.java)

  override fun run(configuration: DiscoveryConfiguration?, context: Context?, namespace: Namespace?) {
    log.info("Bootstrapping Discovery Gateway")

    val tenant = configuration!!.tenantResolver.resolve()

    val vertx = Vertx.vertx()

    val jwt = configuration.buildJWTAuthProvider(vertx)
    configuration.deployRegistry(vertx, jwt, tenant, Handler {  })

    System.`in`.read()
  }
}