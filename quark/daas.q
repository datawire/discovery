package daas 0.1.0;

include daas_proto.q;
use ./quark-improvements.q;

import quark.concurrent;
import quark.reflect;
import quark_ext1;

import daas.proto;

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

      from daas import Discovery, Endpoint
      disco = Discovery.get("https://disco.datawire.io")
      ... bind to port
      disco.register(Endpoint("service", "address", "version"))
      ... serve stuff

    Client:

      from daas import Discovery, Endpoint
      disco = Discovery.get("https://disco.datawire.io")
      endpoint = disco.resolve("servicefoo")

      ... create a connection to endpoint.address
      ... use connection
 */

namespace daas {

  @doc("The Endpoint class captures address and metadata information about a")
  @doc("server functioning as a service instance. Endpoint instances are")
  @doc("asynchronously updated as address/metadata information changes.")
  class Endpoint extends nice.Future {
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

      if (metadata != null) {
        result = result + " " + metadata.toString();
      }

      return result;
    }
  }

  @doc("The ServiceInfo class captures address and metadata information about a")
  @doc("group of servers functioning to provide a single server. ServiceInfo")
  @doc("instances are asynchronously updated as address/metadata information changes.")
  class ServiceInfo extends nice.Future {
    String serviceName = null;
    List<Endpoint> endpoints = [];

    int idx = 0;

    Endpoint choose() {
      if (endpoints.size() > 0) {
        int choice = idx % endpoints.size();
        idx = idx + 1;

        return endpoints[choice];
      }
      else {
        return null;
      }
    }
  }

  @doc("The Discovery class functions as a conduit to the discovery service.")
  @doc("Using it, an application can register and/or lookup service instances.")
  class Discovery extends nice.Bindable {

    String url;

    // Services that we advertise for others to use.
    Map<String, ServiceInfo> registered = new Map<String, ServiceInfo>();

    // Services that the discoball says are available, as well as
    // services for which we are awaiting resolution.
    Map<String, ServiceInfo> services = new Map<String, ServiceInfo>();

    static Logger logger = new Logger("Discovery");

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

        logger.debug("uplink starting");
      }
      else {
        logger.debug("uplink already started");
      }

      mutex.release();
    }

    @doc("Stop the uplink to the discovery service.")
    void stop() {
      mutex.acquire();

      if (started) {
        started = false;
        client.stop();

        logger.debug("uplink stopped");
      }
      else {
        logger.debug("uplink already stopped");
      }

      mutex.release();
    }

/*
  TODO:

    - disco.register -> ServiceInfo (renamed), use to communicate error info on registry.
    - make ServiceInfo (renamed) be the mutable, asynchronously updated thing
    - maybe make Endpoint immutable?

*/

    @doc("Register info about a service with the discovery service.")
    void register(Endpoint endpoint) {
      mutex.acquire();

      String serviceName = endpoint.service;
      logger.debug("registering " + serviceName);

      if (!registered.contains(serviceName)) {
        registered[serviceName] = new ServiceInfo();
      }

      registered[serviceName].endpoints.add(endpoint);

      client.register(endpoint);
      mutex.release();
    }

    @doc("Get a ServiceInfo for a given service")
    @doc("NOTE WELL: ServiceInfo is a Future! therefore our return value may")
    @doc("or may not be finished. It is the caller's responsibility to make")
    @doc("sure that the ServiceInfo is finished before doing anything with it.")
    ServiceInfo lookup(String serviceName) {
      ServiceInfo result;

      logger.debug("lookup " + serviceName);

      // Grab the lock...
      mutex.acquire();

      // ...and see if we already know about this service.
      if (services.contains(serviceName)) {
        // We do; return its ServiceInfo. Note that this doesn't mean that
        // said ServiceInfo is finished.
        logger.debug("lookup found cached " + serviceName);
        result = services[serviceName];
      }
      else {
        // This is a brand-new service. Grab a new ServiceInfo for it...
        logger.debug("lookup going for new " + serviceName);

        result = new ServiceInfo();
        result.serviceName = serviceName;        

        // ...add it to the list of services we're paying attention to...
        services[serviceName] = result;

        // ...and ask the discoball to resolve it for us.
        //
        // XXX Shouldn't we drop the lock here?
        logger.debug("lookup starting disco resolution for " + serviceName);
        client.resolve(result);
      }

      // We're finished interacting with the services array, so drop the
      // lock...
      mutex.release();

      // ...and return whatever we found.
      //
      // NOTE WELL: result is a Future. It may or may not be finished here.
      return result;      
    }

    @doc("Resolve a service name into an available service endpoint.")
    @doc("NOTE WELL: Endpoint is a Future! therefore our return value may")
    @doc("or may not be finished. It is the caller's responsibility to make")
    @doc("sure that the Endpoint is finished before doing anything with it.")
    Endpoint resolve(String serviceName) {
      logger.debug("resolve " + serviceName);

      // We return an Endpoint -- which doesn't mean it's a _finished_ Endpoint.
      // You may need to wait for it to be finished.
      Endpoint futureEndpoint = new Endpoint();

      // Look up the service we're interested in.
      ServiceInfo service = self.lookup(serviceName);

      if (service == null) {
        // WTFO?
        logger.debug("resolve: nothing found looking up " + serviceName);
        return null;
      }

      logger.debug("resolve: waiting to finish " + serviceName);

      // OK. The rest of this happens once the ServiceInfo is finished.
      service.then(self.__method__("_continue_resolution"),
                  [ serviceName, futureEndpoint ]);

      // For now, return our future Endpoint.
      logger.debug("resolve: returning futureEndpoint for " + serviceName);

      return futureEndpoint;
    }

    void _continue_resolution(ServiceInfo service, String serviceName,
                              Endpoint futureEndpoint) {
      // OK, we land here when we have a ServiceInfo to work with. Grab the
      // endpoint we're interested in.

      logger.debug("resolve: finished waiting for " + serviceName);

      futureEndpoint.update(service.choose());
      futureEndpoint.finish(null);
    }
  }
}
