package io.datawire.discovery.config

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.*
import com.hazelcast.config.FileSystemXmlConfig
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager


@JsonTypeInfo(include = As.PROPERTY, property = "type", use = Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(name="hazelcast", value = ClusterManagers.Hazelcast::class)
)
sealed class ClusterManagers {
  class Standalone: ClusterManagers() {}

  class Hazelcast @JsonCreator constructor(
      @JsonProperty("configPath") val configPath: String
  ) : ClusterManagers() {

    fun buildClusterManager(): HazelcastClusterManager {
      val config = FileSystemXmlConfig(configPath)
      return HazelcastClusterManager(config)
    }
  }
}