package io.datawire.discovery.v2.model


interface ServiceStore {

  val size: Int

  fun addRecord(record: ServiceRecord, ttl: Long)

  fun removeRecord(key: ServiceKey)

  fun getRecords(): Collection<ServiceRecord>

  fun getRecord(key: ServiceKey): ServiceRecord?

  fun get(key: ServiceKey): ServiceRecord? {
    return getRecord(key)
  }
}