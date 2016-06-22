quark 1.0;

package datawire_discovery 2.0.0;

use discovery_util.q;
include discovery_protocol.q;

import quark.concurrent;
import quark.reflect;

import discovery.protocol;
import discovery_util;  // bring in EnvironmentVariable

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
    - disco.lookup -> Cluster (renamed)
    - disco.resolve -> is convenience for disco.lookup("<service>").choose()
    - disco.register -> Cluster (renamed), use to communicate error info on registry.
    - make Cluster (renamed) be the mutable, asynchronously updated thing
    - maybe make Node immutable?
*/

namespace discovery {
  @doc("A Cluster is a group of providers of (possibly different versions of)")
  @doc("a single service. Each service provider is represented by a Node.")
  class Cluster {
    List<Node> nodes = [];
    int idx = 0;

    @doc("Choose a single Node to talk to. At present this is a simple round")
    @doc("robin.")
    Node choose() {
      if (nodes.size() > 0) {
        int choice = idx % nodes.size();
        idx = idx + 1;
        return nodes[choice];
      }
      else {
        return null;
      }
    }

    @doc("Add a Node to the cluster (or, if it's already present in the cluster,")
    @doc("update its properties).  At present, this involves a linear search, so")
    @doc("very large Clusters are unlikely to perform well.")
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

    @doc("Remove a Node from the cluster, if it's present. If it's not present, do")
    @doc("nothing. Note that it is possible to remove all the Nodes and be left with")
    @doc("an empty cluster.")
    void remove(Node node) {
      int idx = 0;

      while (idx < nodes.size()) {
        Node ep = nodes[idx];
      
        if (ep.address == null || ep.address == node.address) {
          nodes.remove(idx);
          return;
        }

        idx = idx + 1;
      }

      // XXX: should this be an error? as it is, we silently ignore it.
    }

    @doc("Returns true if and only if this Cluster contains no Nodes.")
    bool isEmpty() {
      return (nodes.size() <= 0);
    }

    // XXX: toString() is a disaster for large clusters.
    @doc("Return a string representation of the Cluster.")
    @doc("")
    @doc("WARNING: every Node is represented in the string. Large Clusters will")
    @doc("produce unusably large strings.")
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
  @doc("server functioning as a service instance. Node extends Future, since")
  @doc("some operations involving Nodes can take awhile -- this means that")
  @doc("you must be sure that a given Node has been finished before using")
  @doc("its values. The Quark documentation for Future has more.")
  class Node extends Future {
    @doc("The service name.")
    String service;
    @doc("The service version (e.g. '1.2.3')")
    String version;
    @doc("The address at which clients can reach the server.")
    String address;
    @doc("Additional metadata associated with this service instance.")
    Map<String,Object> properties;

    @doc("Copy properties from some other Node to this one, and finish this Node.")
    void update(Node node) {
      service = node.service;
      version = node.version;
      address = node.address;
      properties = node.properties;
      self.finish(null);
    }

    @doc("Return a string representation of the Node.")
    String toString() {
      // XXX: this doesn't get mapped into __str__, etc in targets
      String result = "Node(";

      if (service == null) {
        result = result + "<unnamed>";
      }
      else {
        result = result + service;
      }

      result = result + ": ";

      if (address == null) {
        result = result + "<unlocated>";
      }
      else {
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
  @doc("Using it, a provider can register itself as providing a particular service")
  @doc("(see the register method) and a consumer can locate a provider for a")
  @doc("particular service (see the resolve method).")
  class Discovery {
    String url;
    String token;

    static Logger logger = new Logger("discovery");

    // Clusters we advertise to the disco service.
    Map<String, Cluster> registered = new Map<String, Cluster>();

    // Clusters the disco says are available, as well as clusters for
    // which we are awaiting resolution.
    Map<String, Cluster> services = new Map<String, Cluster>();

    bool started = false;
    Lock mutex = new Lock();
    DiscoClient client;

    @doc("Construct a Discovery object. You must set the token before doing")
    @doc("anything else; see the withToken() method.")
    Discovery() {
      logger.info("Discovery created!");
    }

    // XXX PRIVATE API.
    @doc("Lock and make sure we have a client established.")
    void _lock() {
      mutex.acquire();

      logger.info("locked");

      if (client == null) {
        client = new DiscoClient(self);
        logger.info("client ho!");
      }
    }

    // XXX PRIVATE API.
    // @doc("Release the lock")
    void _release() {
      mutex.release();
      logger.info("released");
    }

    @doc("Connect to a specific discovery server. Most callers will just want")
    @doc("connect(). After connecting, you must start the uplink with the start()")
    @doc("method.")
    Discovery connectTo(String url) {
      // Don't use self._lock() here -- manage the lock by hand since we're
      // messing with the client by hand.
      mutex.acquire();

      logger.info("will connect to " + url);

      self.url = url;
      self.client = null;

      mutex.release();

      return self;
    }

    @doc("Connect to the default discovery server. If DATAWIRE_DISCOVERY_URL")
    @doc("is in the environment, it specifies the default; if not, we'll talk to")
    @doc("")
    @doc("wss://discovery-beta.datawire.io/")
    @doc("")
    @doc("After connecting, you must start the uplink with the start() method.")
    Discovery connect() {
      EnvironmentVariable ddu = EnvironmentVariable("DATAWIRE_DISCOVERY_URL");
      String url = ddu.orElseGet("wss://discovery-beta.datawire.io");

      return self.connectTo(url);
    }

    @doc("Set the token we'll use to talk to the server. After doing this,")
    @doc("you must tell Discovery which discovery service to talk to using")
    @doc("either connect() or connectTo().")
    Discovery withToken(String token) {
      self._lock();
      logger.info("using token " + token);
      self.token = token;
      self._release();

      return self;
    }

    @doc("Start the uplink to the discovery service.")
    Discovery start() {
      self._lock();

      if (!started) {
        started = true;
        client.start();
      }

      self._release();
      return self;
    }

    @doc("Stop the uplink to the discovery service.")
    Discovery stop() {
      self._lock();

      if (started) {
        started = false;
        client.stop();
      }

      self._release();
      return self;
    }

    @doc("Register info about a service node with the discovery service. You must")
    @doc("usually start the uplink before this will do much; see start().")
    Discovery register(Node node) {
      self._lock();

      String service = node.service;

      if (!registered.contains(service)) {
        registered[service] = new Cluster();
      }

      registered[service].add(node);
      client.register(node);

      self._release();
      return self;
    }

    @doc("Resolve a service name into an available service node. You must")
    @doc("usually start the uplink before this will do much; see start().")
    @doc("")
    Node resolve(String service) {
      Node result;
      self._lock();

      if (services.contains(service)) {
        result = services[service].choose();
      }
      else {
        result = new Node();
        result.service = service;
        services[service] = new Cluster();
        services[service].add(result);
        client.resolve(result);
      }

      self._release();
      return result;
    }

    // XXX PRIVATE API -- needs to not be here.
    // @doc("Add a given node.")
    void active(Node node) {
      self._lock();

      String service = node.service;

      logger.info("adding " + node.toString());

      if (!services.contains(service)) {
        services[service] = new Cluster();
      }

      Cluster cluster = services[service];
      cluster.add(node);

      self._release();
    }

    // XXX PRIVATE API -- needs to not be here.
    // @doc("Expire a given node.")
    void expire(Node node) {
      self._lock();

      String service = node.service;

      if (services.contains(service)) {
        Cluster cluster = services[service];

        logger.info("removing " + node.toString() + " from cluster");

        cluster.remove(node);

        if (cluster.isEmpty()) {
          logger.info("removing empty cluster for " + node.toString());
          services.remove(service);
        }
      }

      self._release();
    }
  }
}
