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


import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.app.ApplicationConfiguration
import io.datawire.discovery.gateway.config.GatewayConfiguration
import io.datawire.discovery.gateway.server.VertxFactory
import io.datawire.discovery.gateway.tenant.DiscoveryResolver
import io.datawire.discovery.gateway.tenant.DiscoveryResolverFactory
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject


class DiscoveryGatewayConfiguration @JsonCreator constructor(
    @JsonProperty("gateway") private val gateway: GatewayConfiguration,
    @JsonProperty("discoveryResolver") private val discoveryResolverFactory: DiscoveryResolverFactory
): ApplicationConfiguration() {

  fun buildDiscoveryResolver(): DiscoveryResolver {
    return discoveryResolverFactory.build()
  }

  fun buildVertx(): Vertx {
    return VertxFactory().build()
  }

  fun buildGatewayVerticle(): Pair<DiscoveryGatewayVerticle, JsonObject> {
    return gateway.buildGatewayVerticle()
  }
}