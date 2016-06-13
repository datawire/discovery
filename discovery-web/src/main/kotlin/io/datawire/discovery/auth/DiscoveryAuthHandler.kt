/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.discovery.auth

import io.vertx.core.http.HttpHeaders
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl

/**
 * Specialized authentication handler for the Discovery server that allows alternative mechanisms for passing
 * credentials to the server, for example, by URI query parameters.
 *
 * @author plombardi@datawire.io
 * @since 1.0
 */

class DiscoveryAuthHandler(authProvider     : AuthProvider,
                           private val skip : String?) : JWTAuthHandlerImpl(authProvider, skip) {

  override fun handle(context: RoutingContext) {

    // Filed an improvement request with vertx-auth -- https://github.com/vert-x3/vertx-auth/issues/69
    //
    // The skip behavior in parent class is sort of funky in the sense that it checks if skip contains the request path.
    // If the request path is '/' then it will always match. That is too eager. I think the correct behavior is to see
    // if the request path starts with skip and if it does then skip. Some additional code here could check different
    // situations like */${skip} or ${skip}/* but for now this is good enough.

    /* temporarily disabled for team dev reasons; moved health handler up in priority
    if (skip != null && context.request().path().startsWith(skip, true)) {
      context.next()
      return
    }
    */

    context.request()?.getParam("token")?.let { token ->
      context.request().headers().set(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
    }

    super.handle(context)
  }
}