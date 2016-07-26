/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.discovery.model

import io.vertx.core.json.JsonObject
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Contains referential information about a Tenant.
 *
 * @property id the internal tenant identifier.
 * @property user the user associated with the tenant.
 *
 * @constructor creates a reference with a given identifier and optionally an associated user. No consistency checks are
 *              performed to ensure the user is associated to the tenant.
 *
 * @author Philip Lombardi plombardi@datawire.io
 */

data class TenantInfo(val id: String, val user: String = TenantInfo.NONE_USER) {

  /**
   * Constructs a new [TenantInfo] from from the provided [JsonObject]. The structure of the JSON is expected to
   * conform to the following schema:
   *
   * `
   * {
   *   "id": String,
   *   "user": String?
   * }
   * `
   */
  constructor(json: JsonObject): this(json.getString("id"), json.getString("user", TenantInfo.NONE_USER))

  /**
   * Returns a JSON representation of the object which is useful for sending over the Vert.x EventBus as a message for
   * other registered Vert.x consumers to process.
   *
   * The JSON schema is documented below. A [TenantInfo] is not required to have a user set therefore the "user"
   * field in the JSON will default to the value of [TenantInfo.NONE_USER] when this value is not specified on the
   * source object.
   *
   * `
   * {
   *   "id": String,
   *   "user": String
   * }
   * `
   */
  fun toJson(): JsonObject = JsonObject().put("id", id).put("user", user)

  override fun toString() = "$id|$user"

  internal companion object {

    /**
     * The value used to indicate a non-user scoped [TenantInfo].
     */
    const val NONE_USER = "none"
  }
}