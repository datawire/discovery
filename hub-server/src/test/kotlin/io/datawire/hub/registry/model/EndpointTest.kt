package io.datawire.hub.registry.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.datawire.hub.test.HubTest
import io.datawire.util.test.Fixtures
import org.junit.Test

import org.assertj.core.api.Assertions.*


class EndpointTest : HubTest {

  private val objectMapper = ObjectMapper().registerKotlinModule()

  override val fixtures = Fixtures()

  @Test
  fun deserializeFromJson_ReturnEndpoint() {
    val json = fixtures.loadFixtureAsString("valid_Endpoint.json", Charsets.UTF_8)
    val endpoint = objectMapper.readValue(json, Endpoint::class.java)
    //assertThat(endpoint.name).isEqualTo("foobar")
    assertThat(endpoint.scheme).isEqualTo("https")
    assertThat(endpoint.host).isEqualTo("10.0.1.20")
    assertThat(endpoint.port).isEqualTo(443)
  }
}