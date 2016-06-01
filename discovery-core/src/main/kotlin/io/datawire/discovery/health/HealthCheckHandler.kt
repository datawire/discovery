package io.datawire.discovery.health

import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext

private const val CONTENT_TYPE = "text/plain"
private const val CACHE_CONTROL = "private, no-cache, no-store, must-revalidate"

abstract class HealthCheckHandler : Handler<HttpServerRequest> {

  abstract override fun handle(request: HttpServerRequest?)

  protected fun processResult(request: HttpServerRequest,
                              succeeded: Boolean,
                              results: Map<String, HealthCheck.Result>) {

    if (results.isNotEmpty()) {
      val status = if (succeeded && isAllHealthy(results)) OK else SERVICE_UNAVAILABLE
      configureCommonHttpResponse(request.response(), status)
    } else {
      configureCommonHttpResponse(request.response(), NOT_IMPLEMENTED)
    }

    if (request.method() != HttpMethod.HEAD) {
      request.response().end(request.response().statusMessage)
    } else {
      request.response().end()
    }
  }

  protected fun processException(request: HttpServerRequest, cause: Throwable) {
    configureCommonHttpResponse(request.response(), SERVICE_UNAVAILABLE)

    if (request.method() != HttpMethod.HEAD) {
      request.response().end(SERVICE_UNAVAILABLE.reasonPhrase() + ": " + cause.message)
    } else {
      request.response().end()
    }
  }

  private fun isAllHealthy(results: Map<String, HealthCheck.Result>): Boolean {
    for (result in results) {
      if (!result.value.healthy) {
        return false
      }
    }

    return true
  }

  private fun configureCommonHttpResponse(response: HttpServerResponse, status: HttpResponseStatus) {
    response
        .putHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE)
        .putHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)

    response.statusCode = status.code()
    response.statusMessage = status.reasonPhrase()
  }
}