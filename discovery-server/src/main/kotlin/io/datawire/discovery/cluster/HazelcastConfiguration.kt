package io.datawire.discovery.cluster

import com.fasterxml.jackson.annotation.JsonProperty
import com.hazelcast.config.Config
import com.hazelcast.config.FileSystemXmlConfig


/**
 * Provides support for configuring Hazelcast.
 *
 * @author plombardi@datawire.io
 */

data class HazelcastConfiguration(@JsonProperty("configPath") private val path: String) {

  /**
   * Returns a Hazelcast [Config] object based on the properties of this instance.
   */
  fun buildHazelcastConfig(): Config {
    return FileSystemXmlConfig(path)
  }
}