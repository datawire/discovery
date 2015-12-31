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

    // todo(plombardi): TENANT CHECK LOGIC
    //
    // Need logic that ensures JWT.sub == Assigned Hub Tenant. Should just be a matter of comparing the JWT.sub to the
    // assigned tenant ID provided in the configuration file.
    context.user()?.let {
      if (it.principal().getString("sub") != tenant.id) {
        context.fail(401)
      }
    }
  }
}