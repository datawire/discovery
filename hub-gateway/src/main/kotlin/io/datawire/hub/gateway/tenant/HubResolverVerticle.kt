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

package io.datawire.hub.gateway.tenant


import io.vertx.core.AbstractVerticle
import io.vertx.core.logging.LoggerFactory


class HubResolverVerticle(private val resolver: HubResolver): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(EC2InstanceHubResolver::class.java)

  override fun start() {
    log.info("Starting Hub Address Resolver")
    vertx.eventBus().localConsumer<String>("hub-resolver") { lookup ->
      val tenantId = lookup.body()
      val addresses = resolver.resolve(tenantId)

      if (addresses.isNotEmpty()) {
        lookup.reply(addresses.first())
      } else {
        lookup.fail(1, "NO_HUBS_FOUND")
      }
    }
  }
}