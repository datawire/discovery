package io.datawire.hub.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * Represents a container of one or more [Message] instances being sent from clients. Also allows for some additional
 * metadata information to be tracked.
 *
 * @author Philip Lombardi <plombardi@datawire.io>
 */

data class Envelope (

    /**
     * Identifies the sender of the messages contained within.
     */
    @JsonProperty("agent")
    val agent: String,

    /**
     * Messages being delivered for processing.
     */
    @JsonProperty("messages")
    val messages: Collection<Message>
) {

  /**
   * Randomly assigned ID for the envelope which makes it possible to track throughout the system.
   */
  val id = UUID.randomUUID()

  fun messageCount() = messages.size
}