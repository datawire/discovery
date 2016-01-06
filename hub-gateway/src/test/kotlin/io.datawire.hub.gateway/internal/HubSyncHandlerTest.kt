package io.datawire.hub.gateway.internal

import io.datawire.quark.runtime.JSONObject
import org.junit.Test

import org.assertj.core.api.Assertions.*


class HubSyncHandlerTest {

  @Test fun testHubMapping() {
    val json = """{
  "type": "sync",
  "services": {
    "foo": [
      {
        "name": "foo",
        "address": {
          "host": "127.0.0.1",
          "type": "ipv4"
        },
        "port": {
          "name": "hub",
          "port": 52689,
          "secure": true
        },
        "path": "/"
      }
    ],
    "bar": [
      {
        "name": "bar",
        "address": {
          "host": "127.0.0.1",
          "type": "ipv4"
        },
        "port": {
          "name": "hub",
          "port": 52688,
          "secure": true
        },
        "path": "/"
      }
    ]
  }
}"""

    val sync = hub.message.Synchronize(JSONObject())
    sync.data = json

    val map = hashMapOf<String, String>()
    val handler = HubSyncHandler(map)
    handler.onSynchronize(sync)

    assertThat(map).hasSize(2)
    assertThat(map["foo"]).isEqualTo("ws://127.0.0.1:52689/")
    assertThat(map["bar"]).isEqualTo("ws://127.0.0.1:52688/")
  }
}