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

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.datawire.app.ApplicationConfiguration
import io.datawire.discovery.auth.JWTAuthProviderFactory
import io.datawire.discovery.registry.ServiceRegistry
import io.datawire.discovery.registry.ServiceRegistryFactory
import io.datawire.discovery.registry.ServiceRegistryVerticle
import io.datawire.discovery.tenant.TenantResolver
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth


class DiscoveryConfiguration @JsonCreator constructor(
    @JsonProperty("jsonWebToken") private val jsonWebTokenFactory: JWTAuthProviderFactory,
    @JsonProperty("serviceRegistry") private val serviceRegistryFactory: ServiceRegistryFactory,
    @JsonProperty("tenantResolver") val tenantResolver: TenantResolver
): ApplicationConfiguration() {
  fun buildJWTAuthProvider(vertx: Vertx): JWTAuth {
    return jsonWebTokenFactory.build(vertx)
  }

  fun deployRegistry(vertx: Vertx, jwt: JWTAuth, tenant: String, onCompletion: Handler<AsyncResult<String>>) {
    return serviceRegistryFactory.deploy(vertx, jwt, tenant, onCompletion)
  }
}