package io.datawire.discovery.health


class DummyHealthCheck(val result: HealthCheck.Result) : HealthCheck() {
  override fun check(): Result {
    return result
  }
}