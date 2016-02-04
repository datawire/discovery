package io.datawire.discovery.test

import com.fasterxml.jackson.databind.ObjectMapper
import io.datawire.configuration.ConfigurationFactory
import io.datawire.util.test.Fixtures
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTOptions
import java.nio.charset.Charset


interface DiscoveryTest {

  val fixtures: Fixtures

  fun <T> buildConfiguration(clazz: Class<T>, fixture: String, charset: Charset = Charsets.UTF_8): T {
    val yamlDocument = fixtures.loadFixtureAsString(fixture, charset)
    val configFactory = ConfigurationFactory(clazz, ObjectMapper())
    return configFactory.build(yamlDocument, charset)
  }
}