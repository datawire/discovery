package daas 0.1.0;

include daas_proto.q;

import quark.concurrent;
import quark.reflect;

import daas.proto;

namespace daas {

    @doc("The Endpoint class captures address and metadata information about a")
    @doc("server functioning as a service instance. Endpoint instances are")
    @doc("asynchronously updated as address/metadata information changes.")
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
	    // hmm, we could remove this in favor of putting
	    // onActive/onExpire here
	}

        String toString() {
            // XXX: this doesn't get mapped into __str__, etc in targets
            String result = "Endpoint(";
            if (service == null) {
                result = result + "<unnamed>";
            } else {
                result = result + service;
            }
            result = result + ": ";
            if (address == null) {
                result = result + "<unlocated>";
            } else {
                result = result + address;
            }
            if (version != null) {
                result = result + ", " + version;
            }
            result = result + ")";

            if (metadata != null) {
                result = result + " " + metadata.toString();
            }
            return result;
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

}
