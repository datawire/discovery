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

/**
 * Resolves zero or more Datawire Discovery addresses that a Discovery client can communicate with.
 *
 * @since 1.0
 * @author plombardi@datawire.io
 */

interface DiscoveryResolver {

  /**
   * Resolves zero or more Discovery addresses.
   *
   * @param tenant the internal tenant identifier.
   * @return a Set containing zero or more addresses of discovery servers that the tenant has access to use.
   */
  fun resolve(tenant: String): Set<String>
}