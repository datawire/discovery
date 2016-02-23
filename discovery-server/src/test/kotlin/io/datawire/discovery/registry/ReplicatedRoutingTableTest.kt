package io.datawire.discovery.registry

import com.hazelcast.config.ClasspathXmlConfig
import com.hazelcast.core.*
import io.datawire.discovery.registry.model.Endpoint
import io.datawire.discovery.registry.model.ServiceKey
import org.junit.After
import org.junit.Before
import org.junit.Test

import com.jayway.awaitility.Awaitility.*
import com.jayway.awaitility.Duration
import io.datawire.discovery.registry.model.ServiceRecord

import org.assertj.core.api.Assertions.*
import org.junit.Ignore
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ReplicatedRoutingTableTest {

  private val hazelcastConfig = ClasspathXmlConfig("hazelcast-test.xml")

  private lateinit var primaryRoutingTable: ReplicatedRoutingTable

  private val hazelcasts = mutableListOf<HazelcastInstance>()

  @Before
  fun setup() {
    val hazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig)
    hazelcasts.add(hazelcast)

    primaryRoutingTable = ReplicatedRoutingTable(hazelcast)
  }

  @After
  fun teardown() {
    for (hazelcast in hazelcasts) {
      hazelcast.lifecycleService.shutdown()
    }

    hazelcasts.clear()
  }

  @Test
  fun addService_recordIsReplicatedToSecondaryRoutingTable() {
    val otherHazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig)
    hazelcasts.add(otherHazelcast)

    val secondaryRoutingTable = ReplicatedRoutingTable(otherHazelcast)

    val endpoint = Endpoint("ws", "localhost", 52689)
    val serviceKey = ServiceKey("datawire.io", "disco", endpoint)

    primaryRoutingTable.addService(serviceKey, endpoint)

    with()
        .pollInterval(5, TimeUnit.SECONDS)
        .await("secondary-routing-table")
        .timeout(Duration.ONE_MINUTE)
        .until {
          assertThat(secondaryRoutingTable.contains(serviceKey)).isTrue()
        }
  }

  @Test
  @Ignore
  fun heartbeatPreventRecordFromBeingEvicted() {
    val scheduler = Executors.newSingleThreadScheduledExecutor()

    val endpoint = Endpoint("ws", "localhost", 52689)
    val serviceKey = ServiceKey("datawire.io", "disco", endpoint)

    primaryRoutingTable.addService(serviceKey, endpoint)

    val rawTable = primaryRoutingTable.getRoutingTable("datawire.io")
    rawTable.addEntryListener(object : EntryListener<ServiceKey, ServiceRecord> {
      override fun mapEvicted(p0: MapEvent?) { }
      override fun mapCleared(p0: MapEvent?) { }
      override fun entryAdded(p0: EntryEvent<ServiceKey, ServiceRecord>?) { }
      override fun entryRemoved(p0: EntryEvent<ServiceKey, ServiceRecord>?) { }

      override fun entryEvicted(p0: EntryEvent<ServiceKey, ServiceRecord>?) {
        println("EVICTED ENTRY -- ${p0?.key}")
      }

      override fun entryUpdated(p0: EntryEvent<ServiceKey, ServiceRecord>?) { }
    })

    var heartbeats = 0
    scheduler.scheduleWithFixedDelay({
      if (heartbeats < 8) {
        println("Heartbeat $heartbeats")
        primaryRoutingTable.updateLastContactTime(serviceKey)
        //        val result = rawTable.containsKey(serviceKey)
        //        println("Contains Key? -> $result")
        heartbeats++
      }
    }, 15, 15, TimeUnit.SECONDS)

    with()
        .pollInterval(5, TimeUnit.SECONDS)
        .await("primary-routing-table")
        .timeout(Duration.FIVE_MINUTES)
        .until {
          assertThat(primaryRoutingTable.contains(serviceKey)).isFalse()
        }
  }

  //TODO: The TTL should be configurable
  @Test
  fun recordIsEvictedAfterSomePeriodOfTime() {
    val endpoint = Endpoint("ws", "localhost", 52689)
    val serviceKey = ServiceKey("datawire.io", "disco", endpoint)

    primaryRoutingTable.addService(serviceKey, endpoint)

    with()
        .pollInterval(5, TimeUnit.SECONDS)
        .await("primary-routing-table")
        .timeout(Duration.FIVE_MINUTES)
        .until {
          assertThat(primaryRoutingTable.contains(serviceKey)).isFalse()
        }
  }

  @Test
  fun heartbeatsKeepRecordsInRoutingTable() {
    val endpoint = Endpoint("ws", "localhost", 52689)
    val serviceKey = ServiceKey("datawire.io", "disco", endpoint)

    primaryRoutingTable.addService(serviceKey, endpoint)

    with()
        .pollInterval(15, TimeUnit.SECONDS)
        .await()
  }
}