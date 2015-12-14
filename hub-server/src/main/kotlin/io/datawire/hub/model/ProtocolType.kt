package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonCreator


enum class ProtocolType {

  /**
   * Indicates the protocol is IPv4.
   */
  IPv4,

  /**
   * Indicates the protocol is IPv6
   */
  IPv6;

  companion object {
    @JsonCreator @JvmStatic fun fromValue(type: String): ProtocolType {
      when {
        type.toLowerCase() == "ipv4" -> return IPv4
        type.toLowerCase() == "ipv6" -> return IPv6
        else -> throw IllegalArgumentException("unknown network address protocol type (type: $type)")
      }
    }
  }
}