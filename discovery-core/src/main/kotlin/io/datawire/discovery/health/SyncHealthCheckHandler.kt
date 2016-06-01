package io.datawire.discovery.health

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerRequest


class SyncHealthCheckHandler(private val vertx: Vertx,
                             private val registry: HealthCheckRegistry) : HealthCheckHandler() {

  override fun handle(request: HttpServerRequest?) {
    checkNotNull(request, { "HTTP request is null" }).let { request ->
      vertx.executeBlocking<Map<String, HealthCheck.Result>>(
          { future ->
            val results = registry.runHealthChecks()
            future.complete(results)
          },
          { result ->
            if (result.succeeded() || result.cause() == null) {
              processResult(request, result.succeeded(), result.result())
            } else {
              processException(request, result.cause())
            }
          })
    }
  }
}