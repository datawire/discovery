package io.datawire.discovery.health

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

class RoutingContextAdapter(private val delegate: HealthCheckHandler) : Handler<RoutingContext> {
  override fun handle(routingContext: RoutingContext?) = delegate.handle(routingContext?.request())
}