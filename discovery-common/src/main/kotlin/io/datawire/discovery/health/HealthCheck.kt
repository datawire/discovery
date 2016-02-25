package io.datawire.discovery.health


abstract class HealthCheck {

  data class Result private constructor(val isHealthy: Boolean, val message: String?, val error: Throwable?) {

    companion object {
      private val HEALTHY = Result(true, null, null)

      fun healthy(): Result {
        return HEALTHY
      }

      fun healthy(message: String): Result {
        return Result(true, message, null)
      }

      fun healthy(message: String, vararg args: Any): Result {
        return healthy(String.format(message, *args))
      }

      fun unhealthy(message: String): Result {
        return Result(false, message, null)
      }

      fun unhealthy(message: String, vararg args: Any): Result {
        return unhealthy(String.format(message, *args))
      }

      fun unhealthy(error: Throwable): Result {
        return Result(false, error.message, error)
      }
    }
  }

  @Throws(Exception::class)
  protected abstract fun check(): Result

  fun execute(): Result {
    try {
      return check()
    } catch (any: Exception) {
      return Result.unhealthy(any)
    }
  }
}