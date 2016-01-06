package io.datawire.hub.gateway

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.web.Router
import io.vertx.core.logging.LoggerFactory

class HubAuthenticationVerticle(private val jwt: JWTAuth): AbstractVerticle() {

  private val log = LoggerFactory.getLogger(HubAuthenticationVerticle::class.java)


  // mock data until the real auth stuff is ready.
  private val tenants = mapOf(
      "datawire" to "notasecret",
      "pal-corp" to "alsonotasecret"
  )

  private val jsonContentType = "application/json; charset=utf-8"

  override fun start() {
    log.info("starting hub gateway verticle...")

    val router = Router.router(vertx)
    val server = vertx.createHttpServer()

    registerHealthCheck(router)
    registerAuthenticationHandler(router)

    server.requestHandler { router.accept(it) }.listen(8080)
  }

  private fun registerHealthCheck(router: Router) {
    router.get("/health").handler { rc ->
      rc.response().setStatusCode(200).end()
    }
  }

  private fun registerAuthenticationHandler(router: Router) {

    router.post("/v1/authenticate").produces(jsonContentType).handler { rc ->
      val request = rc.request()!!
      val response = rc.response()!!

      val tenant = request.getParam("id")
      val key = request.getParam("key")

      val payload = tenants[tenant]?.let { realKey ->
        if (realKey == key) {
          val json = JsonObject()
          json.put("jwt", jwt.generateToken(JsonObject().put("sub", tenant), JWTOptions().setAlgorithm("HS256")))
        } else {
          null
        }
      }

      if (payload != null) {
        vertx.eventBus().send<String>("hub-lookup", tenant) {
          if (it.succeeded()) {
            payload.put("hub", it.result().body())
            response.setStatusCode(200).end(payload.encode())
          } else {
            response.setStatusCode(500)
          }
        }
      } else {
        response.setStatusCode(401).end()
      }
    }
  }
}