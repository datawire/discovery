package io.datawire.hub

import io.datawire.hub.model.ServiceEndpoint
import io.vertx.core.AsyncResult
import io.vertx.core.shareddata.AsyncMap
import io.vertx.core.shareddata.Lock
import io.vertx.core.shareddata.SharedData


class AsyncServiceRegistry(private val shared: SharedData) {

  // todo(plombardi): for development only; remove we'll need to come up with a way of tracking the services better
  private val serviceGroups = hashSetOf<String>()

  private fun getRegistry(): AsyncMap<String, Set<ServiceEndpoint>> {
    var result: AsyncMap<String, Set<ServiceEndpoint>>? = null

    shared.getClusterWideMap<String, Set<ServiceEndpoint>>("service-registry") {
      if (it.succeeded()) {
        result = it.result()
      } else {
        throw RuntimeException("unable to get service registry")
      }
    }

    return result!!
  }

  fun toMap() {
    val registry = getRegistry()
    val map = hashMapOf<String, Set<ServiceEndpoint>>()
    for (group in serviceGroups) {
      registry.get(group) {
        map.put(group, it.result())
      }
    }
  }

  fun addService(group: String, address: ServiceEndpoint) {
    val registry = getRegistry()
    getRegistry().putIfAbsent(group, hashSetOf()) { putServices ->
      if (putServices.succeeded()) {
        lockService(group, 10) { lock ->
          if (lock.succeeded()) {
            val services = putServices.result().toHashSet()
            services.add(address)
            registry.replace(group, services.toSet()) { replaceServices ->
              if (replaceServices.succeeded()) {
                println("service added")
                serviceGroups.add(group)
              } else {
                println("service addition failed!")
              }
            }
          } else {
            println("failed to acquire lock")
          }
        }
      } else {
        println("failed to put-get services")
      }
    }
  }

  fun lockService(group: String, timeout: Long, body: (AsyncResult<Lock>) -> Unit) {
    shared.getLockWithTimeout("service:$group", timeout, body)
  }
}