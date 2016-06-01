package io.datawire.discovery.health

import java.util.*


class RandomHealthCheck : HealthCheck() {
  override fun check(): Result {
    return if (Random().nextBoolean()) Result.healthy() else Result.unhealthy("Randomness was not in your favor!")
  }
}