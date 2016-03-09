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


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.datawire.util.test.Fixtures
import org.assertj.core.api.Assertions.*
import org.junit.Test

class SimpleDiscoveryResolverTest {

  private val fixtures = Fixtures()

  private val resolver = SimpleDiscoveryServerResolver(setOf("10.0.1.10:52689", "10.0.1.11:52689", "10.0.2.10:52689"))

  @Test
  fun resolve_tenantExists_returnSetOfTenantDiscoveryServers() {
    assertThat(resolver.resolve("DEPRECATED")).hasSameElementsAs(listOf("10.0.1.10:52689", "10.0.1.11:52689", "10.0.2.10:52689"))
  }

  @Test
  fun buildFromValidYaml_returnsExpectedResolver() {
    val yamlDocument = fixtures.loadFixture("valid_SimpleDiscoveryResolver.yml")
    val objectMapper = ObjectMapper(YAMLFactory())
    val newResolver = objectMapper.readValue(yamlDocument, DiscoveryResolverFactory::class.java).build()
    assertThat(newResolver).isEqualTo(this.resolver)
  }
}