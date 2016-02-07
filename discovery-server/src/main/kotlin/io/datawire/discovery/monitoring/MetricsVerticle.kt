package io.datawire.discovery.monitoring

import com.amazonaws.util.json.JSONObject
import io.vertx.core.AbstractVerticle
import io.vertx.ext.dropwizard.MetricsService
import io.vertx.ext.web.Router


class MetricsVerticle: AbstractVerticle() {

  override fun start() {
    val metricsService = MetricsService.create(vertx)
    val router = Router.router(vertx)
    router.get("/metrics").produces("application/json").handler {
      val json = metricsService.getMetricsSnapshot(vertx)
      it.response().putHeader("content-type", "application/json").end(json.toString())
    }

    val server = vertx.createHttpServer()
    server.requestHandler { router.accept(it) }.listen(8079)
  }
}