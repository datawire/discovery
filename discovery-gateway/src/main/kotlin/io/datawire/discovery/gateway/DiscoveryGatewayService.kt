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

package io.datawire.discovery.gateway


import io.datawire.app.Application
import io.datawire.app.Initializer
import io.datawire.discovery.gateway.command.ServerCommand


class DiscoveryGatewayService: Application<DiscoveryGatewayConfiguration>("discovery-gateway", DiscoveryGatewayConfiguration::class.java) {

  override fun initialize(initializer: Initializer<DiscoveryGatewayConfiguration>?) {
    initializer!!.addCommand(ServerCommand(this))
  }

  companion object {

    init {
      configureComponentsProperties()
    }

    @JvmStatic fun main(args: Array<String>) {
      DiscoveryGatewayService().run(*args)
    }

    /**
     * Configure internal components which expose system properties to control features, for example, choice of logging
     * framework or phone home capabilities.
     */
    private fun configureComponentsProperties() {
      // hazelcast configuration
      System.setProperty("hazelcast.logging.type", "slf4j")
      System.setProperty("hazelcast.phone.home.enabled", "false")

      // vertx configuration
      System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
    }
  }
}