package io.datawire.hub.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.datawire.hub.event.RegistryEvent
import io.datawire.hub.model.ServiceEndpoint
import io.datawire.hub.model.ServiceKey
import io.vertx.core.eventbus.EventBus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Class responsible for mapping service names to one or more service entries.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */


class ServiceRegistry(
    private val mapper: ObjectMapper,
    private val events: EventBus
) {

  private val services: ConcurrentMap<ServiceKey, ServiceEndpoint> = ConcurrentHashMap()
  private val subscribers: Set<String> = ConcurrentHashMap.newKeySet<String>()

  fun run() {
    events.consumer<RegistryEvent>("hub.services") { msg ->
      onRegistryEvent(msg.body())
    }
  }

  private fun onRegistryEvent(event: RegistryEvent) {
    when(event) {
      is RegistryEvent.AddServiceEndpointEvent -> {
        val existing = services.putIfAbsent(event.endpoint.key, event.endpoint)
        if (existing != null) {
          services.replace(event.endpoint.key, existing, event.endpoint)
        }

        broadcast(mapper.writeValueAsString(getServices()))
      }
      is RegistryEvent.RemoveServiceEndpointEvent -> {
        services.remove(event.endpoint.key)

        broadcast(mapper.writeValueAsString(getServices()))
      }
    }
  }

  private fun broadcast(data: String) {
    for (sub in subscribers) {
      events.publish(sub, data)
    }
  }

  private fun getServices(): Map<String, Set<ServiceEndpoint>> {
    return services.entries.fold(hashMapOf()) { acc, entry ->
      val endpoints = acc.putIfAbsent(entry.key.name, hashSetOf()) as MutableSet
      endpoints.add(entry.value)
      return acc
    }
  }
}