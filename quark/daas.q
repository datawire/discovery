package daas 0.1.0;

import quark.concurrent;
import quark.reflect;

namespace daas {

    @doc("The Endpoint class captures address and metadata information")
    @doc("for a server functioning as a service instance.")
    class Endpoint extends Future {
        @doc("The service name.")
        String service;
        @doc("The service version.")
        String version;
        @doc("The address from which clients can reach the server.")
        String address;
        @doc("Additional metadata associated with this service instance.")
        Map<String,Object> metadata;

	void update(Endpoint endpoint) {
	    service = endpoint.service;
	    version = endpoint.version;
	    address = endpoint.address;
	    metadata = endpoint.metadata;
	    // hmm, if we need a mutex, then we can't toJSON anymore
	    // without fixes to ignore non JSONable stuff..., we could
	    // remove this in favor of putting onActive/onExpire here
	}
    }

    class Endpoints {
        List<Endpoint> endpoints = [];
        int idx = 0;

        Endpoint choose() {
            if (endpoints.size() > 0) {
                int choice = idx % endpoints.size();
                idx = idx + 1;
                return endpoints[choice];
            } else {
                return null;
            }
        }
    }

    @doc("The Discovery class functions as a conduit to the discovery service.")
    @doc("Using it, an application can register and/or lookup service instances.")
    class Discovery {

        String url;

        // Endpoints we advertise to the disco service.
        Map<String,Endpoints> registered = new Map<String,Endpoints>();
        // Endpoints the disco says are available, as well as
        // endpoints for which we are awaiting resolution.
        Map<String,Endpoints> endpoints = new Map<String,Endpoints>();

        bool started = false;
        Lock mutex = new Lock();
        DiscoClient client;

        Discovery(String url) {
            self.url = url;
            client = new DiscoClient(self);
        }

	@doc("Start the uplink to the discovery service.")
        void start() {
            mutex.acquire();
            if (!started) {
                started = true;
                client.start();
            }
            mutex.release();
        }

	@doc("Stop the uplink to the discovery service.")
        void stop() {
            mutex.acquire();
            if (started) {
                started = false;
                client.stop();
            }
            mutex.release();
        }

        @doc("Register info about a service endpoint with the discovery service.")
        void register(Endpoint endpoint) {
            mutex.acquire();
            String service = endpoint.service;
            if (!registered.contains(service)) {
                registered[service] = new Endpoints();
            }
            registered[service].endpoints.add(endpoint);
            client.register(endpoint);
            mutex.release();
        }

        @doc("Resolve a service name into an available service endpoint.")
        Endpoint resolve(String service) {
            Endpoint result;
            mutex.acquire();
            if (endpoints.contains(service)) {
                result = endpoints[service].choose();
            } else {
		result = new Endpoint();
		result.service = service;
		endpoints[service] = new Endpoints();
		endpoints[service].endpoints.add(result);
		client.resolve(result);
            }
            mutex.release();
            return result;
        }

    }

    class DiscoClient extends DiscoHandler, HTTPHandler, WSHandler, Task {

        Discovery disco;
	float restartDelay = 0.1;

        DiscoClient(Discovery discovery) {
            disco = discovery;
        }

        void start() {
            schedule(0.0);
        }

        void stop() {
            // We've been asked to stop, so cancel the request/close
            // the web socket if it is open.

            // ...
        }

        void schedule(float time) {
            Context.runtime().schedule(self, 0.0);
        }

        void register(Endpoint endpoint) {
            // Trigger send of delta if we are connected, otherwise do
            // nothing because the full set of endpoints will be
            // resent when we connect/reconnect.

            // ...
        }

        void resolve(Endpoint endpoint) {
            // Right now the disco protocol will notify about any
            // endpoint, so we don't need to do anything here, if we
            // wanted to change this, we'd have to track the set of
            // points we are interested in resolving and communicate
            // as this changes.

	    // Hmm, maybe we actually need to do something to deal
	    // with timeouts.
        }

	void onActive(Active active) {
	    // Stick the endpoint in the available set.

	    Endpoint endpoint = active.endpoint;
	    String service = endpoint.service;

	    if (!disco.endpoints.contains(service)) {
		disco.endpoints[service] = new Endpoints();
	    }

	    List<Endpoint> endpoints = disco.endpoints[service].endpoints;
	    int idx = 0;
	    bool updated = false;
	    while (idx < endpoints.size()) {
		Endpoint ep = endpoints[idx];
		if (ep.address == endpoint.address) {
		    ep.update(endpoint);
		    updated = true;
		    ep.finish(null);
		    break;
		}
		idx = idx + 1;
	    }
	    if (!updated) {
		endpoints.add(endpoint);
		endpoint.finish(null);
	    }
	}

	void onExpire(Expire expire) {
	    // Remove the endpoint from our available set.

	    // hmm, we could make all Endpoint objects we hand out be
	    // continually updated until they expire...

	    Endpoint endpoint = expire.endpoint;
	    String service = endpoint.service;

	    if (disco.endpoints.contains(service)) {
		// XXX: no way to remove from List or Map
		// ...
	    }

	    // ...
	}

        void onExecute(Runtime runtime) {
            // Do our periodic chores here, this will involve checking
            // the desired state held by disco against our actual
            // state and taking any measures necessary to address the
            // difference:
	    //
	    //  - if we aren't connected and we are supposed to be,
	    //    then kick off the connection sequence
	    //  - if we haven't sent a heartbeat recently enough, then
	    //    do that

            // ...
        }

        void onHTTPInit(HTTPRequest request) { /* unused */ }
        void onHTTPResponse(HTTPRequest request, HTTPResponse response) {
	    // Check the response for the websocket url and connect to it.
	}
        void onHTTPError(HTTPRequest request, String message) {
	    // XXX: How do we report the error?
	}
        void onHTTPFinal(HTTPRequest request) { /* unused */ }

        void onWSInit(WebSocket socket) { /* unused */ }
        void onWSConnected(WebSocket socket) {
	    // Whenever we (re)connect, notify the server of any
	    // endpoints we have registered.

	    // ...
	}
        void onWSMessage(WebSocket socket, String message) {
	    // Decode and dispatch incoming messages.
	    Event event = Event.decode(message);
	    disco.mutex.acquire();
	    event.dispatch(self);
	    disco.mutex.release();
	}
        void onWSBinary(WebSocket socket, Buffer message) { /* unused */ }

        void onWSClosed(WebSocket socket) { /* unused */ }

        void onWSError(WebSocket socket) {
	    // XXX: Should log the error here.
	}

        void onWSFinal(WebSocket socket) {
	    disco.mutex.acquire();
	    if (disco.started) {
		schedule(restartDelay);
	    }
	    disco.mutex.release();
	}

    }

    interface DiscoHandler {

	void onActive(Active active);

	void onExpire(Expire expire);

    }

    class Event {

	static Event decode(String message) {
	    JSONObject json = message.parseJSON();
	    String type = json["type"];
	    Class clazz = Class.get(type);
	    Event event = ?clazz.construct([]);
	    fromJSON(clazz, event, json);
	    return event;
	}

	void dispatch(DiscoHandler handler);
    }

    class Active extends Event {

	Endpoint endpoint;

	void dispatch(DiscoHandler handler) {
	    handler.onActive(self);
	}
    }

    class Expire extends Event {

	Endpoint endpoint;

	void dispatch(DiscoHandler handler) {
	    handler.onExpire(self);
	}
    }

}
