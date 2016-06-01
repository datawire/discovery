package io.datawire.discovery.health

import org.junit.Before
import org.junit.Test

import org.assertj.core.api.Assertions.*

class HealthCheckRegistryTest {

  private lateinit var healthChecks: HealthCheckRegistry

  @Before
  fun setup() {
    healthChecks = HealthCheckRegistry()
  }

  @Test
  fun trueIfHealthCheckIsInRegistry() {
    healthChecks.register("DUMMY", DummyHealthCheck(HealthCheck.Result.healthy()))
    assertThat(healthChecks.contains("DUMMY")).isTrue()
    assertThat(healthChecks.isRegistered("DUMMY")).isTrue()
  }

  @Test
  fun falseIfHealthCheckIsNotInRegistry() {
    assertThat(healthChecks.contains("DUMMY")).isFalse()
    assertThat(healthChecks.isRegistered("DUMMY")).isFalse()
  }

  @Test
  fun registeringAndDeregisteringHealthChecksModifiesInternalState() {
    healthChecks.register("DUMMY", DummyHealthCheck(HealthCheck.Result.healthy()))
    assertThat(healthChecks.contains("DUMMY")).isTrue()

    healthChecks.deregister("DUMMY")
    assertThat(healthChecks.contains("DUMMY")).isFalse()
  }

  @Test
  fun registeringAndDeregisteringServiceModifiesRegistrySize() {
    healthChecks.register("DUMMY", DummyHealthCheck(HealthCheck.Result.healthy()))
    assertThat(healthChecks.size()).isEqualTo(1)

    healthChecks.deregister("DUMMY")
    assertThat(healthChecks.size()).isEqualTo(0)
  }
}