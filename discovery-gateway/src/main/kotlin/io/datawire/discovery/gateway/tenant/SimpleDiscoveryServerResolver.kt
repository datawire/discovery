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


import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Implementation of [DiscoveryResolver] that returns a pre-configured set of Discovery addresses for a tenant. This is not
 * particularly useful in a production scenario but is very handy in a development or test environment.
 *
 * @author plombardi@datawire.io
 * @since 1.0
 */

data class SimpleDiscoveryServerResolver(private val discoveryServers: Map<String, Set<String>>) : DiscoveryResolver {

  override fun resolve(tenant: String): Set<String> {
    return discoveryServers[tenant] ?: emptySet()
  }

  data class Factory(@JsonProperty("servers") private val servers: Map<String, Set<String>>): DiscoveryResolverFactory {
    override fun build(): DiscoveryResolver {
      return SimpleDiscoveryServerResolver(servers)
    }
  }
}