/*
 * Copyright 2015, 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use self file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * The official Quark implementation for the Datawire Hub Registry.
 */

@version("1.0.0")
package hub {

  @doc("Contains Hub domain model interfaces and classes.")
  package model {

    @doc("Indicates the object can be encoded as JSON.")
    interface ToJSON {
    
      @doc("Convert a POQO into its JSON representation. Implementations are NOT required to serialize all fields into the resulting JSONObject.")
      JSONObject toJSON();
    }

    @doc("Indicates an endpoint or route that provides a service.")
    class Endpoint extends ToJSON {

      @doc("The DNS name or IP address of the service endpoint.")
      String host;
      
      @doc("The port number of the service endpoint.")
      int port;
      
      @doc("The URI scheme for the endpoint.")
      String scheme;
      
      @doc("The URI representation of the endpoint.")
      String uri;

      @doc("Construct a new Endpoint.")
      Endpoint(String scheme, String host, int port, String uri) {
        self.scheme = scheme;
        self.host = host;
        self.port = port;
        self.uri = uri;
      }
      
      @doc("Return the DNS name or IP address of the service endpoint.")
      String getHost() {
        return host;
      }
      
      @doc("Return the port number of the service endpoint.")
      int getPort() {
        return port;
      }

      @doc("Return the URI scheme (protocol) of the service endpoint.")
      String getScheme() {
        return scheme;
      }
      
      @doc("Return the URI representation of the service endpoint.")
      String getURI() {
        return uri;
      }

      @doc("Return a String representation of the endpoint.")
      String toString() {
        return scheme + "://" + host + ":" + port.toString();
      }

      static Endpoint create(String scheme, String host, int port) {
        return new Endpoint(scheme, host, port, null);
      }

      @doc("Returns a JSON representation of the endpoint information. The canonical URI is NOT included as it can be logically assembled by this objects fields.")
      JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json["scheme"] = scheme;
        json["host"] = host;
        json["port"] = port;
        return json;
      }
    }
     
    @doc("A routing table is contains zero or more pairs of service routes and their associated communication endpoints.")
    class RoutingTable {
    
      @doc("The raw map of service names to service endpoints. The individual entries in the Map are logically known as 'routes'.")
      Map<String, List<Endpoint>> routes = new Map<String, List<Endpoint>>();

      @doc("Returns a list of all services defined in the routing table.")
      List<String> getServices() {
        List<String> result = routes.keys();
        if (result != null) {
          return result;
        } else {
          return new List<String>();
        }
      }

      @doc("Retrieve all the communication endpoints for a given service.")
      List<Endpoint> getEndpoints(String name) {
        List<Endpoint> endpoints = routes[name];
        if (endpoints != null) {
          return endpoints;
        } else {
          return new List<Endpoint>();
        }
      }
      
      @doc("Add another endpoint to a given service.")
      RoutingTable addEndpoint(String name, Endpoint endpoint) {
        if (routes[name] == null) {
          List<Endpoint> endpoints = new List<Endpoint>();
          routes[name] = endpoints;
        }
        
        routes[name].add(endpoint);        
        return self;
      }
      
      @doc("Indicates whether a route exists for a service within the routing table.")
      bool hasRoute(String name) {
        // todo(issue: https://github.com/datawire/quark/issues/106) -> switch data[name] != null to data.contains(name)
        return routes[name] != null && (routes[name].size() > 0);
      }
            
      @doc("Returns a count of endpoints available for a specific service.")
      int count(String name) {
        List<Endpoint> endpoints = routes[name];
        if (endpoints != null) {
          return endpoints.size();
        } else {
          return 0;
        }
      }
    }
    
    @doc("Factory interface for an object that can create RoutingTable from JSON.")
    interface RoutingTableFactory {
      
      @doc("Construct a RoutingTable from JSON.")
      RoutingTable create(JSONObject json);
    }
    
    @doc("Default implementation of RoutingTableFactory.")
    class DefaultRoutingTableFactory extends RoutingTableFactory {
      
      RoutingTable create(JSONObject json) {
        RoutingTable result = new RoutingTable();

        List<String> services = json["services"].keys();
        int idx = 0;
        while(idx < services.size()) {
          String serviceName = services[idx];
          JSONObject endpoints = json["services"][serviceName];

          int jdx = 0;
          while(jdx < endpoints.size()) {
            Endpoint endpoint = endpointFromJSON(endpoints.getListItem(jdx));
            result.addEndpoint(serviceName, endpoint);
            jdx = jdx + 1;
          }

          idx = idx + 1;
        }

        return result;
      }
      
      @doc("[Internal]: Construct an Endpoint object from JSON")
      Endpoint endpointFromJSON(JSONObject json) {
        return new Endpoint(json["scheme"], json["host"], json["port"], json["uri"]);
      }
    }
  }

  @doc("Contains Hub client events (connect, disconnect, etc.)")
  package event {

    @doc("A base class for Hub client events")
    class BaseEvent {
    
      long occurrenceTime = now();
      
      long getOccurrenceTime() {
        return occurrenceTime;
      }
    }

    @doc("Indicates the client has connected to a Hub server")
    class Connected extends BaseEvent {
      
      Connected() { }

      void dispatch(registry.RegistryHandler handler) {
        handler.onConnected(self);
      }
    }

    @doc("Indicates the client has disconnected from a Hub server")
    class Disconnected extends BaseEvent {
      
      Disconnected() { }

      void dispatch(registry.RegistryHandler handler) {
        handler.onDisconnected(self);
      }
    }
  }

  @doc("Contains Hub client messages (register, deregister, subscribe, routes etc.)")
  package message {

    @doc("The base message type")
    class BaseMessage {
    
      @doc("The message type")
      String type = "unknown";
      
      @doc("The origin of the message")
      String origin = null;
      
      @doc("The ID of the message")
      String id = null;

      BaseMessage(String type) {
        self.type = type;
      }

      void dispatch(registry.RegistryHandler handler) {
        handler.onMessage(self);
      }

      JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json["type"] = self.type;
        
        if (origin != null) {
          json["origin"] = self.origin;
        }
        
        if (id != null) {
          json["id"] = self.origin;
        }
        
        return json;
      }

      String toString() {
        return "BaseMessage(type=" + type + ")";
      }
    }

    @doc("A message indicating a service endpoint should be added")
    class RegisterServiceRequest extends BaseMessage {

      String name;
      model.Endpoint endpoint;

      RegisterServiceRequest(String name, model.Endpoint endpoint) {
          super("register");
          self.name = name;
          self.endpoint = endpoint;
      }

      JSONObject toJSON() {
          JSONObject json = super.toJSON();

          json["name"] = name;
          if (endpoint != null) {
              json["endpoint"] = endpoint.toJSON();
          } else {
              json["endpoint"] = null;
          }

          return json;
      }

      void dispatch(registry.RegistryHandler handler) {
          handler.onMessage(self);
      }
    }

    @doc("A message indicate a service endpoint should be removed.")
    class DeregisterServiceRequest extends BaseMessage {

      String name;
      model.Endpoint endpoint;

      DeregisterService(String name, model.Endpoint endpoint) {
          super("remove-service");
          self.name = name;
          self.endpoint = endpoint;
      }

      JSONObject toJSON() {
          JSONObject json = super.toJSON();

          json["name"] = name;
          if (endpoint != null) {
              json["endpoint"] = endpoint.toJSON();
          } else {
              json["endpoint"] = null;
          }

          return json;
      }

      void dispatch(registry.RegistryHandler handler) {
          handler.onMessage(self);
      }
    }

    @doc("A message indicating a service is still alive and well.")
    class HeartbeatNotification extends BaseMessage {

      HeartbeatNotification() {
          super("heartbeat");
      }

      void dispatch(registry.RegistryHandler handler) {
          handler.onHeartbeat(self);
      }
    }
    
    @doc("A message indicating a client is interested in receiving the current services routes")
    class RoutesRequest extends BaseMessage {
      
      RoutesRequest() {
        super("routes");
      }
      
      void dispatch(registry.RegistryHandler handler) {
        handler.onRoutesRequest(self);
      }
    }
    
    @doc("A message containing a mapping of service names to current known routes")
    class RoutesResponse extends BaseMessage {
      
      model.RoutingTable routes;
      
      RoutesResponse(model.RoutingTable routes) {
        super("routes");
        self.routes = routes;
      }
      
      model.RoutingTable getRoutes() {
        return routes;
      }
      
      void dispatch(registry.RegistryHandler handler) {
        handler.onRoutesResponse(self);
      }
    }

    @doc("A message indicating a client is interested in subscribing to the services registry")
    class Subscribe extends BaseMessage {
      
      Subscribe() {
          super("subscribe");
      }

      void dispatch(registry.RegistryHandler handler) {
        handler.onSubscribe(self);
      }
    }

    @doc("A message indicating the server or client experienced an error")
    class HubError extends BaseMessage {

      @doc("A unique authoritative code for the error")
      int code;

      @doc("A short string that identifies the code. Preference should be given to the code number instead of the name")
      String codeName;

      HubError(int code, String codeName) {
        super("hub-error");
        self.code = code;
        self.codeName = codeName;
      }
    }

    interface MessageFactory {
      message.BaseMessage create(JSONObject json);
    }

    class DefaultMessageFactory extends MessageFactory {
    
      model.RoutingTableFactory routesFactory = new model.DefaultRoutingTableFactory();
    
      message.BaseMessage create(JSONObject json) {
        String type = json["type"];
      
        if (type == "routes") { 
          model.RoutingTable routes = routesFactory.create(json);
          return new message.RoutesResponse(routes);
        }

        return new message.BaseMessage(type);
      }
    }
  }

  package registry {

    @doc("Handler for Datawire Registry messages and events")
    interface RegistryHandler {

      @doc("Invoked when a client error is experienced")
      void onClientError(message.HubError error) {
        self.onMessage(error);
      }

      @doc("Invoked when a client connects")
      void onConnected(event.Connected connected) {
        self.onEvent(connected);
      }

      @doc("Invoked when a client disconnects")
      void onDisconnected(event.Disconnected disconnected) {
        self.onEvent(disconnected);
      }

      @doc("Invoked when an error occurs. The error could be server OR client side. See onServerError() or onClientError() for handling a specific type")
      void onError(message.HubError error) {
        if (error.code > 999  && error.code < 2000) { self.onServerError(error); }
        if (error.code > 1999 && error.code < 3000) { self.onClientError(error); }
      }

      @doc("Generic event handler")
      void onEvent(event.BaseEvent event) {
        // generic do-nothing handler
      }

      @doc("Invoked after a heartbeat notification is sent to the Hub")
      void onHeartbeat(message.HeartbeatNotification heartbeat) {
        self.onMessage(heartbeat);
      }

      @doc("Generic message handler")
      void onMessage(message.BaseMessage message) {
        // generic do-nothing handler
      }

      @doc("Invoked when service routes are requested from the Hub")
      void onRoutesRequest(message.RoutesRequest routes) {
        self.onMessage(routes);
      }

      @doc("Invoked when service routes are delivered from the Hub")
      void onRoutesResponse(message.RoutesResponse response) {
        self.onMessage(response);
      }

      @doc("Invoked when a Hub server error occurs")
      void onServerError(message.HubError error) {
        self.onMessage(error);
      }

      @doc("Invoked after a client sends a subscribe message to the Hub")
      void onSubscribe(message.Subscribe subscribe) {
        self.onMessage(subscribe);
      }
    }

    @doc("Default handler implementation for Datawire Registry messages")
    class DefaultRegistryHandler extends RegistryHandler { }

    @doc("Defines the basic Hub client contract")
    interface RegistryClient {

      @doc("Connect to the registry server.")
      void connect();

      @doc("Disconnect from the registry server.")
      void disconnect();

      @doc("Indicates whether the client is connected to the registry server.")
      bool isConnected();

      @doc("Returns the entire routing table.")
      model.RoutingTable getRoutingTable();

      @doc("Returns a list of endpoints.")
      List<model.Endpoint> getRoutes(String name);

      @doc("Checks to see whether a route exists for the given service name.")
      bool hasRoute(String name);

      @doc("Registers a service endpoint.")
      void registerEndpoint();

      @doc("Deregisters a service endpoint.")
      void deregisterEndpoint();

      @doc("Sends a heartbeat to the registry server.")
      void heartbeat();

      @doc("Subscribe to the registry server and receive updates about changes to the routing table.")
      void subscribe();
    }

    @doc("Defines the communication semantics between the client and server.")
    class BasicRegistryClient extends RegistryClient, RegistryHandler, WSHandler {

      @doc("Quark runtime")
      Runtime runtime;

      @doc("WebSocket connection")
      WebSocket socket;

      @doc("The URL of the registry server")
      String registryUrl;

      @doc("The token to use for authentication with the registry server")
      String token;

      @doc("The service endpoint routing table")
      model.RoutingTable routes;

      @doc("The service name that an Endpoint is associated with.")
      String serviceName;

      @doc("The endpoint associated with a specific Service.")
      model.Endpoint endpoint;

      @doc("Constructs new registry Quark value objects from JSON")
      message.MessageFactory messageFactory = new message.DefaultMessageFactory();

      BasicRegistryClient(Runtime runtime, String url, String token, String serviceName, model.Endpoint endpoint) {
        self.runtime = runtime;
        self.registryUrl = url;
        self.token = token;
        self.routes = new model.RoutingTable();
        self.serviceName = serviceName;
        self.endpoint = endpoint;
      }

      void connect() {
        if (socket == null) {
          runtime.open(registryUrl + "?token=" + token, self);
        }
      }

      void deregisterEndpoint() {
        send(new message.DeregisterServiceRequest(serviceName, endpoint));
      }

      void disconnect() {
        if (socket != null) {
          socket.close();
        }
      }

      model.RoutingTable getRoutingTable() {
        return routes;
      }

      List<model.Endpoint> getRoutes(String name) {
        return routes.getEndpoints(name);
      }

      bool hasRoute(String name) {
        return routes.hasRoute(name);
      }

      bool isConnected() {
        return socket != null;
      }

      void registerEndpoint() {
        send(new message.RegisterServiceRequest(serviceName, endpoint));
      }

      void heartbeat() {
        send(new message.HeartbeatNotification());
      }

      void onRoutesResponse(message.RoutesResponse response) {
        self.routes = response.routes;
      }

      void send(message.BaseMessage message) {
        if (message != null && isConnected()) {
          JSONObject json = message.toJSON();
          socket.send(json.toString());
        }
      }

      void subscribe() {
        send(new message.Subscribe());
      }

      // ---------------------------------------------------------------------------------------------------------------
      // INTERNAL
      // ---------------------------------------------------------------------------------------------------------------

      void onWSConnected(WebSocket socket) {
        self.socket = socket;
        event.Connected connected = new event.Connected();
        connected.dispatch(self);
      }

      void onWSClosed(WebSocket socket) {
        socket = null;
        event.Disconnected disconnected = new event.Disconnected();
        disconnected.dispatch(self);
      }

      void onWSMessage(WebSocket socket, String raw) {
        JSONObject json = raw.parseJSON();
        message.BaseMessage message = messageFactory.create(json);
        message.dispatch(self);
      }
    }

    @doc("Provides a client that can communicate with the Datawire Cloud Hub.")
    class CloudRegistryClient extends BasicRegistryClient, HTTPHandler {

      GatewayOptions gateway = null;

      CloudRegistryClient(Runtime runtime, GatewayOptions gateway, String serviceName, model.Endpoint endpoint) {
        super(runtime, null, null, serviceName, endpoint);
        self.gateway = gateway;
      }

      void connect() {
        if (self.socket == null && gateway.authenticate) {
          HTTPRequest request = new HTTPRequest(gateway.buildUrl());
          request.setMethod("POST");
          request.setHeader("Authorization", "Bearer " + gateway.getToken());
          self.runtime.request(request, self);
        }
      }

      // ---------------------------------------------------------------------------------------------------------------
      // INTERNAL
      // ---------------------------------------------------------------------------------------------------------------

      void onHTTPResponse(HTTPRequest request, HTTPResponse response) {
        if (response.getCode() == 200) {
          JSONObject connectionInfo = response.getBody().parseJSON();
          self.registryUrl = connectionInfo["url"];
          self.runtime.open(self.registryUrl + "?token=" + gateway.getToken(), self);
        } else {
          message.HubError error = new message.HubError(response.getCode(), "http-error");
          error.dispatch(self);
        }
      }
    }

    @doc("Connection options for the Hub Gateway.")
    class GatewayOptions {

      @doc("Indicates whether HTTPS should be used or not.")
      bool secure = true;

      @doc("Indicates that the client should authenticate with the Gateway.")
      bool authenticate = true;

      @doc("The DNS name or IP of the Hub Gateway service.")
      String gatewayHost = "hub-gw.datawire.io";

      @doc("The port of the running Hub Gateway service.")
      int gatewayPort = 443;

      @doc("The path to the Hub Gateway connector.")
      String gatewayConnectorPath = "/v1/connect";

      @doc("The key token to use when connecting to the Hub Gateway")
      String token = "";

      GatewayOptions(String token) {
        self.token = token;
      }

      String getToken() {
        return token;
      }

      GatewayOptions setToken(String token) {
        self.token = token;
        return self;
      }

      String buildUrl() {
        String scheme = "https";
        int gatewayPort = self.gatewayPort;

        if (secure) {
          if (gatewayPort == null || gatewayPort < 1 || gatewayPort > 65535) {
            gatewayPort = 443;
          }
        } else {
          scheme = "http";
          if (gatewayPort == null || gatewayPort < 1 || gatewayPort > 65535) {
            gatewayPort = 80;
          }
        }

        return scheme + "://" + gatewayHost + ":" + gatewayPort.toString() + gatewayConnectorPath;
      }
    }
  }

  /*
  @doc("Contains Hub client interfaces and classes")
  package client {

    @doc("Default handler implementation for Datawire Hub messages")
    class DefaultHubHandler extends HubHandler { }

    @doc("Basic Hub client that must be extended to provide features beyond connecting to the Hub")
    class HubClient extends HTTPHandler, HubHandler, Task, WSHandler {

      @doc("Quark runtime")
      Runtime runtime;

      @doc("WebSocket connection")
      WebSocket socket;

      @doc("Hub Gateway configuration")
      GatewayOptions gateway;

      @doc("Indicates whether the Hub Gateway should be used.")
      bool useGateway = true;

      @doc("The URL of the Datawire Hub")
      String hubUrl;
      
      message.MessageFactory messageFactory = new message.DefaultMessageFactory();

      HubClient(Runtime runtime, GatewayOptions gateway) {
        self.runtime = runtime;
        self.gateway = gateway;
      }

      void connect() {
        if (socket == null && gateway.authenticate) {
          HTTPRequest request = new HTTPRequest(gateway.buildUrl());
          request.setMethod("POST");
          request.setHeader("Authorization", "Bearer " + gateway.getToken());
          runtime.request(request, self);
        }
      }

      void disconnect() {
        if (socket != null) {
          socket.close();
        }
      }

      @doc("The task to run")
      void onExecute(Runtime runtime) { }

      bool isConnected() {
        return socket != null;
      }

      @doc("Schedules the task for execution after the specified period")
      void schedule(float period) {
        runtime.schedule(self, period);
      }

      @doc("Schedules the task for execution immediately")
      void scheduleNow() {
        runtime.schedule(self, 0.0);
      }
        
      void send(message.BaseMessage message) {
        if (message != null && isConnected()) {
          JSONObject json = message.toJSON();
          socket.send(json.toString());
        }
      }

      void onHTTPResponse(HTTPRequest request, HTTPResponse response) {
        if (response.getCode() == 200) {
          JSONObject connectionInfo = response.getBody().parseJSON();
          hubUrl = connectionInfo["url"];
          runtime.open(hubUrl + "?token=" + gateway.getToken(), self);
        } else {
          message.HubError error = new message.HubError(response.getCode(), "http-error");
          error.dispatch(self);
        }
      }

      void onWSConnected(WebSocket socket) {
        self.socket = socket;
        event.Connected connected = new event.Connected();
        connected.dispatch(self);
      }

      void onWSClosed(WebSocket socket) {
        socket = null;
        event.Disconnected disconnected = new event.Disconnected();
        disconnected.dispatch(self);
      }

      void onWSMessage(WebSocket socket, String raw) {
        JSONObject json = raw.parseJSON();
        message.BaseMessage message = messageFactory.create(json);
        message.dispatch(self);
      }
    }
    
    @doc("Implementation of the Hub client that persists the Route table delivered by the Hub")
    class PersistentHubClient extends HubClient {
    
      @doc("The service endpoint routing table")
      model.RoutingTable routes;

      @doc("The service name that an Endpoint is associated with.")
      String serviceName;

      @doc("The endpoint associated with a specific Service.")
      model.Endpoint endpoint;

      @doc("Constructs a new instance of a Hub client.")
      PersistentHubClient(Runtime runtime, GatewayOptions gateway) {
        super(runtime, gateway);
        self.routes = new model.RoutingTable();
      }

      @doc("Configures a service endpoint that can be published by this client.")
      PersistentHubClient configureEndpoint(String serviceName, model.Endpoint endpoint) {
        self.serviceName = serviceName;
        self.endpoint = endpoint;
        return self;
      }

      @doc("Returns the entire routing table.")
      model.RoutingTable getRoutingTable() {
        return routes;
      }
      
      @doc("Returns a list of endpoints.")
      List<model.Endpoint> getRoutes(String name) {
        return routes.getEndpoints(name);
      }
      
      bool hasRoute(String name) {
        return routes.hasRoute(name);
      }

      void registerEndpoint() {
        self.send(new message.RegisterServiceRequest(serviceName, endpoint));
      }

      void deregisterEndpoint() {
        self.send(new message.DeregisterServiceRequest(serviceName, endpoint));
      }

      void heartbeat() {
        self.send(new message.HeartbeatNotification());
      }
      
      void onRoutesResponse(message.RoutesResponse response) {
        self.routes = response.routes;
      }
    }

    @doc("Handler for Datawire Hub messages and events")
    interface HubHandler {
    
      @doc("Invoked when a client error is experienced")
      void onClientError(message.HubError error) {
        self.onMessage(error);
      }

      @doc("Invoked when a client connects")
      void onConnected(event.Connected connected) {
        self.onEvent(connected);
      }

      @doc("Invoked when a client disconnects")
      void onDisconnected(event.Disconnected disconnected) {
        self.onEvent(disconnected);
      }

      @doc("Invoked when an error occurs. The error could be server OR client side. See onServerError() or onClientError() for handling a specific type")
      void onError(message.HubError error) {
        if (error.code > 999  && error.code < 2000) { self.onServerError(error); }
        if (error.code > 1999 && error.code < 3000) { self.onClientError(error); }
      }
      
      @doc("Generic event handler")
      void onEvent(event.BaseEvent event) {
        // generic do-nothing handler
      }

      @doc("Invoked after a heartbeat notification is sent to the Hub")
      void onHeartbeat(message.HeartbeatNotification heartbeat) {
        self.onMessage(heartbeat);
      }

      @doc("Generic message handler")
      void onMessage(message.BaseMessage message) {
        // generic do-nothing handler
      }
      
      @doc("Invoked when service routes are requested from the Hub")
      void onRoutesRequest(message.RoutesRequest routes) {
        self.onMessage(routes);
      }
      
      @doc("Invoked when service routes are delivered from the Hub")
      void onRoutesResponse(message.RoutesResponse response) {
        self.onMessage(response);
      }

      @doc("Invoked when a Hub server error occurs")
      void onServerError(message.HubError error) {
        self.onMessage(error);
      }

      @doc("Invoked after a client sends a subscribe message to the Hub")
      void onSubscribe(message.Subscribe subscribe) {
        self.onMessage(subscribe);
      }
    }

    @doc("Connection options for the Hub Gateway")
    class GatewayOptions {
      
      @doc("Indicates whether HTTPS should be used or not")
      bool secure = true;
      
      @doc("Indicates that the client should authenticate with the Gateway")
      bool authenticate = true;

      @doc("The DNS name or IP of the Hub Gateway service")
      String gatewayHost = "hub-gw.datawire.io";
      
      @doc("The port of the running Hub Gateway service")
      int gatewayPort = 443;
      
      @doc("The path to the Hub Gateway connector")
      String gatewayConnectorPath = "/v1/connect";

      String token = "";

      GatewayOptions(String token) {
        self.token = token;
      }

      String getToken() {
        return token;
      }
      
      GatewayOptions setToken(String token) {
        self.token = token;
        return self;
      }

      @doc("")
      String buildUrl() {
        String scheme = "https";
        int gatewayPort = self.gatewayPort;

        if (secure) {
          if (gatewayPort == null || gatewayPort < 1 || gatewayPort > 65535) {
            gatewayPort = 443;
          }
        } else {
          scheme = "http";
          if (gatewayPort == null || gatewayPort < 1 || gatewayPort > 65535) {
            gatewayPort = 80;
          }
        }

        return scheme + "://" + gatewayHost + ":" + gatewayPort.toString() + gatewayConnectorPath;
      }
    }
  }
  */
}