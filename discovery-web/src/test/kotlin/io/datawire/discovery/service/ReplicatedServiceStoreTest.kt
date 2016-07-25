package io.datawire.discovery.service

import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import org.junit.After
import org.junit.Before

/**
 * Tests that replication works
 *
 */


class ReplicatedServiceStoreTest {

  private lateinit var hazelcastInstances: List<HazelcastInstance>

  private lateinit var primaryServiceStore: ReplicatedServiceStore
  private lateinit var secondaryServiceStore: ReplicatedServiceStore

  @Before
  fun setup() {
    val hazelcast1 = Hazelcast.newHazelcastInstance()
    val hazelcast2 = Hazelcast.newHazelcastInstance()

    primaryServiceStore = ReplicatedServiceStore(hazelcast1.getReplicatedMap("service-store-0"))
    secondaryServiceStore = ReplicatedServiceStore(hazelcast2.getReplicatedMap("service-store-1"))
  }

  @After
  fun teardown() {
    for (instance in hazelcastInstances) {
      instance.lifecycleService.shutdown()
    }

    hazelcastInstances = emptyList()
  }
}