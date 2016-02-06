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

package io.datawire.discovery

import io.datawire.app.Application
import io.datawire.app.Initializer
import io.datawire.discovery.command.ServerCommand
import io.datawire.discovery.command.SharedServerCommand


class DiscoveryService: Application<DiscoveryConfiguration>("discovery-server", DiscoveryConfiguration::class.java) {
  override fun initialize(initializer: Initializer<DiscoveryConfiguration>?) {
    initializer!!.addCommand(ServerCommand(this))
    initializer!!.addCommand(SharedServerCommand(this))
  }

  companion object {
    @JvmStatic fun main(args: Array<String>) {
      DiscoveryService().run(*args)
    }
  }
}