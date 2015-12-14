package io.datawire.hub.vertx

import io.vertx.core.DeploymentOptions
import io.vertx.core.Verticle


data class VerticleBundle(val verticle: Verticle, val options: DeploymentOptions)