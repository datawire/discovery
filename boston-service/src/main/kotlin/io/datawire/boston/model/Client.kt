package io.datawire.boston.model

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import io.vertx.core.http.ServerWebSocket
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean


data class Client(private val clients: MutableMap<String, Client>,
                  private val services: MutableMap<String, MutableSet<ServiceEndpoint>>,
                  private val socket: ServerWebSocket) {

    private var serviceName: String? = null
    private var serviceEndpoint: ServiceEndpoint? = null
    private var heartbeat: Instant? = null
    private val subscribed: AtomicBoolean = AtomicBoolean(false)

    private companion object {
        private val mapper = ObjectMapper().registerKotlinModule()
    }

    val id = socket.textHandlerID()

    init {
        clients.put(id, this)

        socket.handler { it ->
            val msg = mapper.readValue<Message>(it.toString())
            try {
                handleMessage(msg)
            } catch (ex: JsonParseException) {
                socket.writeFinalTextFrame(mapper.writeValueAsString(Error("dir", 1)))
            }
        }

        socket.closeHandler {
            clients.remove(this.id)

            if (serviceEndpoint != null) {
                services[serviceName!!]?.removeRaw(serviceEndpoint)
            }
        }
    }

    private fun handleMessage(message: Message) {
        when(message) {
            is ServiceRegistration -> {
                // todo(plombardi): FIX!!! -> thread safety issues out the wazoo here!
                if (message.name !in services) {
                    services.put(message.name, hashSetOf())
                }

                serviceName = message.name
                serviceEndpoint = message.endpoint

                services[serviceName!!]?.add(serviceEndpoint!!)
                publishToSubscribed()
            }
            is ServiceQuery -> {}
            is ServiceHeartbeat -> heartbeat = Instant.now()
            is Subscribe -> {
                subscribed.compareAndSet(false, true)
                socket.writeFinalTextFrame(mapper.writeValueAsString(ServiceRegistry(services)))
            }
        }
    }

    private fun publishToSubscribed() {
        for (c in clients.values) {
            if (c.subscribed.get()) {
                c.socket.writeFinalTextFrame(mapper.writeValueAsString(ServiceRegistry(services)))
            }
        }
    }
}