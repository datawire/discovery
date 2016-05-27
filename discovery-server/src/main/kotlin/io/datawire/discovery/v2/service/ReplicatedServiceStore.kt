package io.datawire.discovery.v2.service

import com.hazelcast.core.EntryListener
import com.hazelcast.core.ReplicatedMap
import io.datawire.discovery.v2.model.ServiceKey
import io.datawire.discovery.v2.model.ServiceRecord
import io.datawire.discovery.v2.model.ServiceStore
import java.util.concurrent.TimeUnit


class ReplicatedServiceStore(private val backingMap: ReplicatedMap<String, ServiceRecord>) : ServiceStore {

  override val size: Int
    get() = backingMap.size

  override fun addRecord(record: ServiceRecord, ttl: Long) {
    backingMap.put(record.key.toString(), record, ttl, TimeUnit.SECONDS)
  }

  override fun removeRecord(key: ServiceKey) {
    throw UnsupportedOperationException()
  }

  override fun getRecord(key: ServiceKey): ServiceRecord? {
    throw UnsupportedOperationException()
  }

  override fun getRecords(): Collection<ServiceRecord> {
    return backingMap.values
  }

  fun registerChangeListener(listener: EntryListener<String, ServiceRecord>) {
    backingMap.addEntryListener(listener)
  }
}