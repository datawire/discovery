package datawire_discovery 2.0.0;

include discovery_protocol.q;

import quark.concurrent;
import quark.reflect;

import discovery.protocol;

/*
   Context:

     For phase one, all our user wants to do is have a convenient
     library to get an address to connect to that is backed by a
     realtime discovery service rather than DNS. The mechanism for
     connecting is entirely in the user's code, all we do is provide
     an address (likely in the form of a url, but really it's just an
     opaque string that was advertised by a service instance).

     Behind the scenes we will do client side load balancing and
     possibly fancier routing in the future, but the user doesn't
     observe this directly through the API. The user *does* observe
     this indirectly because they don't need to deploy a central load
     balancer.

     Conceptually, we should strive to be a drop-in replacement for
     dns, with the one difference being that the server process is
     creating the dns record directly rather than a system
     administrator.

   API usage sketch:

     Server:

       from discovery import Discovery, Node
       disco = Discovery.get("https://disco.datawire.io")
       ... bind to port
       disco.register(Node("service", "address", "version"))
       ... serve stuff

     Client:

       from discovery import Discovery, Node
       disco = Discovery.get("https://disco.datawire.io")
       node = disco.resolve("servicefoo")

       ... create a connection to node.address
       ... use connection
 */

/*

  TODO:

    - rename Cluster to something reasonable, e.g. ServiceInfo
    - disco.lookup -> Cluster (renamed)
    - disco.resolve -> is convenience for disco.lookup("<service>").choose()
    - disco.register -> Cluster (renamed), use to communicate error info on registry.
    - make Cluster (renamed) be the mutable, asynchronously updated thing
    - maybe make Node immutable?

*/

namespace discovery {

    @doc("The Cluster class holds a set of nodes associated with the same service.")
    class Cluster {

        List<Node> nodes = [];
        int idx = 0;

        Node choose() {
            if (nodes.size() > 0) {
                int choice = idx % nodes.size();
                idx = idx + 1;
                return nodes[choice];
            } else {
                return null;
            }
        }

        void add(Node node) {
	    int idx = 0;
	    while (idx < nodes.size()) {
		Node ep = nodes[idx];
		if (ep.address == null || ep.address == node.address) {
		    ep.update(node);
		    return;
		}
		idx = idx + 1;
	    }
            nodes.add(node);
        }

        String toString() {
            String result = "Cluster(";
            int idx = 0;
            while (idx < nodes.size()) {
                if (idx > 0) {
                    result = result + ", ";
                }
                result = result + nodes[idx].toString();
                idx = idx + 1;
            }
            result = result + ")";
            return result;
        }

    }

    @doc("The Node class captures address and metadata information about a")
    @doc("server functioning as a service instance.")
    class Node extends Future {

        @doc("The service name.")
        String service;
        @doc("The service version.")
        String version;
        @doc("The address from which clients can reach the server.")
        String address;
        @doc("Additional metadata associated with this service instance.")
        Map<String,Object> properties;

	void update(Node node) {
	    service = node.service;
	    version = node.version;
	    address = node.address;
	    properties = node.properties;
	}

        String toString() {
            // XXX: this doesn't get mapped into __str__, etc in targets
            String result = "Node(";
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

            if (properties != null) {
                result = result + " " + properties.toString();
            }
            return result;
        }

    }

    @doc("The Discovery class functions as a conduit to the discovery service.")
    @doc("Using it, an application can register and/or lookup service instances.")
    class Discovery {

        String url;
        String token;

        // Nodes we advertise to the disco service.
        Map<String,Cluster> registered = new Map<String,Cluster>();
        // Nodes the disco says are available, as well as nodes for
        // which we are awaiting resolution.
        Map<String,Cluster> services = new Map<String,Cluster>();

        bool started = false;
        Lock mutex = new Lock();
        DiscoClient client;

        @doc("The url parameter points to the discovery service.")
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

        @doc("Register info about a service node with the discovery service.")
        void register(Node node) {
            mutex.acquire();
            String service = node.service;
            if (!registered.contains(service)) {
                registered[service] = new Cluster();
            }
            registered[service].add(node);
            client.register(node);
            mutex.release();
        }

        @doc("Resolve a service name into an available service node.")
        Node resolve(String service) {
            Node result;
            mutex.acquire();
            if (services.contains(service)) {
                result = services[service].choose();
            } else {
		result = new Node();
		result.service = service;
		services[service] = new Cluster();
		services[service].add(result);
		client.resolve(result);
            }
            mutex.release();
            return result;
        }

    }

}
