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

package io.datawire.discovery.gateway.tenant


import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory
import java.util.*


class DiscoveryResolverVerticle(private val resolver: DiscoveryResolver): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(DiscoveryResolverVerticle::class.java)
  private val random = Random()

  override fun start() {
    log.info("Starting Discovery Address Resolver")
    vertx.eventBus().localConsumer<String>("discovery-resolver") { lookup ->
      val tenantId = lookup.body()
      val addresses = resolver.resolve(tenantId).toList()
      Collections.shuffle(addresses, random)

      if (addresses.isNotEmpty()) {
        lookup.reply(addresses[0])
      } else {
        lookup.fail(1, "NO_DISCOVERY_SERVERS_FOUND")
      }
    }
  }
}