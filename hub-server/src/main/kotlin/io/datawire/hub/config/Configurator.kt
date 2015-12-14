package io.datawire.hub.config

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import java.nio.file.Path


class Configurator {

  fun load(path: Path) {

  }

  fun build(): HttpServerOptions {
    val result = HttpServerOptions()

    //result.setPort()
    //result.setHost()

    return result
  }
}