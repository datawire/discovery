package io.datawire.discovery.health

import java.util.*
import java.util.concurrent.*


class HealthCheckRegistry {

  private val healthChecks: ConcurrentMap<String, HealthCheck> = ConcurrentHashMap()

  fun size(): Int {
    return healthChecks.size
  }

  fun register(name: String, healthCheck: HealthCheck) {
    healthChecks.putIfAbsent(name, healthCheck)
  }

  fun deregister(name: String) {
    healthChecks.remove(name)
  }

  fun isRegistered(name: String): Boolean {
    return contains(name)
  }

  fun contains(name: String): Boolean {
    return name in healthChecks
  }

  @Throws(NoSuchElementException::class)
  fun runHealthCheck(name: String): HealthCheck.Result {
    val healthCheck = healthChecks[name] ?: throw NoSuchElementException("No health check named $name exists")
    return healthCheck.execute()
  }

  fun runHealthChecks(): SortedMap<String, HealthCheck.Result> {
    val results = TreeMap<String, HealthCheck.Result>()
    for (entry in healthChecks.entries) {
      val result = entry.value.execute()
      results.put(entry.key, result)
    }
    return Collections.unmodifiableSortedMap(results)
  }

  fun getNames(): SortedSet<String> {
    return TreeSet(healthChecks.keys)
  }
}