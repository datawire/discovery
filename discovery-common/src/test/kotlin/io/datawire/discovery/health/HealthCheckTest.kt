package io.datawire.discovery.health

import org.junit.Test

import org.assertj.core.api.Assertions.*

class HealthCheckTest {

  @Test
  fun executePassesThroughUnhealthyCheckResult() {
    val result = HealthCheck.Result.unhealthy("Ruh roh")
    val healthCheck = DummyHealthCheck(result)
    assertThat(healthCheck.execute()).isEqualTo(result)
  }

  @Test
  fun executePassesThroughHealthyCheckResult() {
    val result = HealthCheck.Result.healthy()
    val healthCheck = DummyHealthCheck(result)
    assertThat(healthCheck.execute()).isEqualTo(result)
  }

  @Test
  fun executeCatchesExceptionAndReturnsUnhealthyResult() {
    val badCheck = object : HealthCheck() {
      override fun check(): Result {
        throw RuntimeException("Ruh roh!")
      }
    }

    try {
      val result = badCheck.execute()
      assertThat(result.isHealthy).isFalse()
      assertThat(result.error)
          .isInstanceOf(RuntimeException::class.java)
          .hasMessage("Ruh roh!")
    } catch (any: Exception) {
      fail("Exception was thrown but should have been caught!")
    }
  }
}