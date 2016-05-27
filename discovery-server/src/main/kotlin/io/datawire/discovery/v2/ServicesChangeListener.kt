package io.datawire.discovery.v2

import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.core.MapEvent


class ServicesChangeListener : EntryListener<String, String> {
  override fun entryAdded(event: EntryEvent<String, String>?) = Unit
  override fun entryEvicted(event: EntryEvent<String, String>?) = Unit
  override fun entryRemoved(event: EntryEvent<String, String>?) = Unit
  override fun entryUpdated(event: EntryEvent<String, String>?) = Unit
  override fun mapEvicted(event: MapEvent?) = Unit
  override fun mapCleared(event: MapEvent?) = Unit
}