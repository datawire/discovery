package io.datawire.hub.util

import kotlin.reflect.KClass


data class Jar(private val clazz: KClass<*>) {

  val version: String = getVersion(clazz.java, "0.0.1-UNKNOWN")

  private fun getVersion(clazz: Class<*>, default: String): String {
    return clazz.`package`?.implementationVersion ?: default
  }
}