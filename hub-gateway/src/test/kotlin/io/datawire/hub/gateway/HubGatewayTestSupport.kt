package io.datawire.hub.gateway

import com.fasterxml.jackson.databind.ObjectMapper
import io.datawire.configuration.ConfigurationFactory
import io.datawire.util.test.Fixtures
import java.nio.charset.Charset


class HubGatewayTestSupport {

  private val fixtures = Fixtures()

  fun loadConfiguration(configurationFixture: String, charset: Charset = Charsets.UTF_8): HubGatewayConfiguration {
    val yamlDocument = fixtures.loadFixtureAsString(configurationFixture, charset)
    val configFactory = ConfigurationFactory(HubGatewayConfiguration::class.java, ObjectMapper())
    return configFactory.build(yamlDocument, charset)
  }
}