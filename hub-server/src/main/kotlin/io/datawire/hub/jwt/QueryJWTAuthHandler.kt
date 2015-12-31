package io.datawire.hub.jwt

import io.datawire.hub.tenant.model.TenantId
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl


class QueryJWTAuthHandler(authProvider: AuthProvider, private val tenant: TenantId, skip: String?) : JWTAuthHandlerImpl(authProvider, skip) {
  override fun handle(context: RoutingContext?) {
    context!!.request()!!.getParam("token")?.let { jwt ->
      context.request().headers().add("Authorization", "Bearer $jwt")
    }
    super.handle(context)
    context.user()?.let {
      if (it.principal().getString("sub") != tenant.id) {
        context.fail(401)
      }
    }
  }
}