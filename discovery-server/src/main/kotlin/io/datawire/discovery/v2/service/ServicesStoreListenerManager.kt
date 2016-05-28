package io.datawire.discovery.v2.service


interface ServicesStoreListenerManager {

  /**
   * Adds a listener for a services store.
   *
   * @param tenant the Tenant to add the listener for.
   */
  fun addListener(tenant: String)

  /**
   * Removes a listener for a services store.
   *
   * @param tenant The tenant to remove the listener for.
   */
  fun removeListener(tenant: String)
}