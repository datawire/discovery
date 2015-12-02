package io.datawire.hub

import java.net.ServerSocket


abstract class HubTest {

  /**
   * Retrieves a random port for use in a test. There is a very small chance the port is not available if another
   * server listens on the acquired port within the tiny amount of time between acquisition and the port being returned
   * and used by the caller.
   */
  fun getRandomPort(): Int {
    val socket = ServerSocket(0)
    val port = socket.localPort
    socket.close()
    return port
  }
}