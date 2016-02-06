package io.datawire.discovery.registry

import io.datawire.discovery.registry.model.Endpoint
import io.datawire.discovery.registry.model.ServiceKey
import org.junit.Before
import org.junit.Test

import org.assertj.core.api.Assertions.*
import java.util.concurrent.TimeUnit

class LocalServiceRegistryTest {

  lateinit var registry: LocalServiceRegistry

  @Before
  fun setup() {
    registry = LocalServiceRegistry()
  }

  @Test
  fun size_noServices_ReturnZero() {
    assertThat(registry.size).isEqualTo(0)
  }

  @Test
  fun size_someServices_ReturnValueEqualToCurrentNumberOfServices() {
    val endpoint0 = Endpoint("https", "10.0.1.10", 443)
    val endpoint1 = Endpoint("http", "10.0.1.11", 80)
    val endpoint2 = Endpoint("https", "10.0.1.12", 443)

    registry.addService(ServiceKey("datawire", "foo", endpoint0), endpoint0)
    registry.addService(ServiceKey("datawire", "bar", endpoint1), endpoint1)
    registry.addService(ServiceKey("datawire", "bot", endpoint2), endpoint2)

    assertThat(registry.size).isEqualTo(3)

    registry.removeService(ServiceKey("datawire", "foo", endpoint0))
    assertThat(registry.size).isEqualTo(2)
  }

  @Test
  fun contains_containsTheSpecifiedServiceKey_ReturnTrue() {
    val endpoint0 = Endpoint("https", "10.0.1.10", 443)
    registry.addService(ServiceKey("datawire", "foo", endpoint0), endpoint0)
    assertThat(registry.contains(ServiceKey("datawire", "foo", endpoint0))).isTrue()
  }

  @Test
  fun contains_doesNotContainTheSpecifiedServiceKey_ReturnTrue() {
    val endpoint0 = Endpoint("https", "10.0.1.10", 443)
    assertThat(registry.contains(ServiceKey("datawire", "foo", endpoint0))).isFalse()
  }

  @Test
  fun updateLastContactTime_serviceIsPresent_LastContactTimeIsModified() {
    val endpoint = Endpoint("https", "10.0.1.10", 443)
    val key = ServiceKey("datawire", "foo", endpoint)

    registry.addService(key, endpoint)

    val recordAtTime0 = registry[key]!!

    TimeUnit.MILLISECONDS.sleep(100) // sleep just to ensure the clock moves ahead a little.

    registry.updateLastContactTime(key)
    val recordAtTime1 = registry[key]!!

    assertThat(recordAtTime1.lastHeartbeat.isAfter(recordAtTime0.lastHeartbeat))
  }

  @Test
  fun updateLastContactTime_serviceIsAbsent_DoesNothing() {
    val endpoint = Endpoint("https", "10.0.1.10", 443)
    val key = ServiceKey("datawire", "foo", endpoint)

    registry.updateLastContactTime(key)
    assertThat(registry[key]).isNull()
  }

  @Test
  fun mapNamesToServices_NoServices_ReturnEmptyMap() {
    val mappedServices = registry.mapNamesToEndpoints("datawire")
    assertThat(mappedServices).isEmpty()
  }

  @Test
  fun mapNamesToServices_SeveralServices_ReturnExpectedMapping() {
    val endpoint0 = Endpoint("https", "10.0.1.10", 443)
    val endpoint1 = Endpoint("https", "10.0.1.11", 443)
    val endpoint2 = Endpoint("http", "10.0.1.11", 80)
    val endpoint3 = Endpoint("https", "10.0.1.12", 443)

    registry.addService(ServiceKey("datawire", "foo", endpoint0), endpoint0)
    registry.addService(ServiceKey("datawire", "foo", endpoint1), endpoint1)
    registry.addService(ServiceKey("datawire", "bar", endpoint2), endpoint2)
    registry.addService(ServiceKey("datawire", "bot", endpoint3), endpoint3)

    val mappedServices = registry.mapNamesToEndpoints("tenant")
    assertThat(mappedServices.keys).containsOnly("foo", "bar", "bot")
    assertThat(mappedServices["foo"]).containsOnly(endpoint0, endpoint1)
    assertThat(mappedServices["bar"]).containsOnly(endpoint2)
    assertThat(mappedServices["bot"]).containsOnly(endpoint3)
  }
}