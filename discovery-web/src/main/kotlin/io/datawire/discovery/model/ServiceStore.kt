package io.datawire.discovery.model


interface ServiceStore {

  val size: Int

  fun addRecord(record: ServiceRecord)

  fun removeRecord(key: ServiceKey): Boolean

  fun getRecords(): Collection<ServiceRecord>

  fun getRecord(key: ServiceKey): ServiceRecord?

  fun get(key: ServiceKey): ServiceRecord? {
    return getRecord(key)
  }
}