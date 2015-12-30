@version("1.0.0")
package hub {

    interface ToJSON {
        JSONObject toJSON();
    }
    
    interface fromJSON<T> {
        T fromJSON(JSONObject json);
    }
    
    package model {    
        @doc("Interface for health checks")
        interface HealthCheck {
            bool check();
        }

        @doc("Represents the IP or DNS name associated with a service endpoint")
        class NetworkAddress extends ToJSON {

            String host;
            String type;

            NetworkAddress(String host, String type) {
                self.host = host;
                self.type = type;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json["host"] = host;
                json["type"] = type;
                return json;
            }

            String toString() {
                return host;
            }
        }
        
        @doc("Represents the port associated with a service endpoint and the protocol it handles")
        class ServicePort extends ToJSON {

            String  name;
            int     port;

          ServicePort(String name, int port) {
              self.name = name;
              self.port = port;
          }

          String toString() {
              return name + "-" + port.toString();
          }

          JSONObject toJSON() {
              JSONObject json = new JSONObject();
              json["name"] = name;
              json["port"] = port;
              json["secure"] = false;
              return json;
          }
        }
        
        @doc("Represents the combination of network address and service port.")
        class ServiceEndpoint extends ToJSON {

            String          name;
            String          path = null;
            NetworkAddress  address;
            ServicePort     port;

            ServiceEndpoint(String name, NetworkAddress address, ServicePort port) {
                self.name = name;
                self.address = address;
                self.port = port;
                self.path = "/";
            }

            String toString() {
                return port.name + "://" + address.host + ":" + port.port.toString();
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json["name"] = name;
                json["address"] = address.toJSON();
                json["port"] = port.toJSON();
                json["path"] = path;
                return json;
            }
        }
        
        @doc("Maps a named service to a set of known endpoints.")
        class ServiceRecord {

            String name;
            List<String> endpoints;

            ServiceRecord(String name) {
                self.name = name;
                self.endpoints = new List<String>();
            }

            String toString() {
                String result  = "";

                String header  = "Record: " + name;
                String pointer = " --> ";

                result = result + header;

                // build padding for pretty printing
                String padding = "";
                int pdx = 0;
                while(pdx < header.size()) {
                    padding = padding + " ";
                    pdx = pdx + 1;
                }

                // build the result
                int edx = 0;
                while(edx < endpoints.size()) {
                    if (edx != 0) {
                      result = result + padding;
                    }

                    result = result + pointer + endpoints[edx] + "\n";
                    edx = edx + 1;
                }

                return result;
            }

            JSONObject toJson() {
                JSONObject json = new JSONObject();
                return json;
            }
        }        
    }

    package message {
        
        @doc("The base message type")
        class HubMessage {

            @doc("ID of the message; Not used currently")
            int id = 0;

            @doc("The message type")
            String type = null;

            @doc("The time the message was created")
            long timestamp = now();

            HubMessage(String type) {
                self.type = type;
            }

            JSONObject toJSON() {
                JSONObject json = new JSONObject();
                json["type"] = self.type;
                json["time"] = self.timestamp;
                return json;
            }
            
            void dispatch(HubHandler handler) {
                handler.onHubMessage(self);
            }            
        }
        
        @doc("A message indicating a service endpoint should be added. ")
        class AddService extends HubMessage {

            model.ServiceEndpoint endpoint;

            AddService(model.ServiceEndpoint endpoint) {
                super("add-service");
                self.endpoint = endpoint;
            }

            JSONObject toJSON() {
                JSONObject json = super.toJSON();

                print(json.toString());

                if (endpoint != null) {
                    json["endpoint"] = endpoint.toJSON();
                } else {
                    json["endpoint"] = null;
                }

                return json;
            }
            
            void dispatch(HubHandler handler) {
                handler.onHubMessage(self);
            }            
        }

        @doc("A message indicate a service endpoint should be removed.")
        class RemoveService extends HubMessage {

            model.ServiceEndpoint endpoint;

            RemoveService(model.ServiceEndpoint endpoint) {
                super("remove-service");
                self.endpoint = endpoint;
            }

            JSONObject toJSON() {
                JSONObject json = super.toJSON();

                if (endpoint != null) {
                    json["endpoint"] = endpoint.toJSON();
                } else {
                    json["endpoint"] = null;
                }

                return json;
            }
            
            
            void dispatch(HubHandler handler) {
                handler.onHubMessage(self);
            }             
        }

        @doc("A message indicating a client would like the latest state from the server.")
        class Synchronize extends HubMessage {
            Synchronize() {
                super("synchronize");
            }
            
            
            void dispatch(HubHandler handler) {
                handler.onSynchronize(self);
            }             
        }

        @doc("A message indicating a service is still alive and well.")
        class Heartbeat extends HubMessage {

            Heartbeat() {
                super("heartbeat");
            }
            
            void dispatch(HubHandler handler) {
                handler.onHeartbeat(self);
            }               
        }

        @doc("A message indicating a client is interested in subscribing to the services registry.")
        class Subscribe extends HubMessage {
            Subscribe() {
                super("subscribe");
            }
            
            void dispatch(HubHandler handler) {
                handler.onSubscribe(self);
            }              
        }

        @doc("A message indicating the client has connected")
        class Connected extends HubMessage {
            Connected() {
                super("connected");
            }
            
            void dispatch(HubHandler handler) {
                handler.onConnected(self);
            }              
        }
        
        @doc("A message indicating the client has disconnected")
        class Disconnected extends HubMessage {
            Disconnected() {
                super("disconnected");
            }
            
            void dispatch(HubHandler handler) {
                handler.onDisconnected(self);
            }              
        }
        
        @doc("A message indicating the server or client experienced an error")
        class HubError extends HubMessage {
            
            int     code;
            String  codeName;
        
            HubError(int code, String codeName) {
                super("hub-error");
                self.code = code;
                self.codeName = codeName;
            }
        }
    }
    
    package util {
        @doc("Represents a result of some type")
        class Result<T> {

            T     value;
            Error error;

            Result(T value, Error error) {
                self.value = value;
                self.error = error;
            }

            T getValue() {
                return value;
            }

            Error getError() {
                return error;
            }

            bool isError() {
                return error != null;
            }

            bool isNotError() {
                return !isError();
            }
        }

        @doc("Represents an error. Do not confuse with HubError")
        class Error {
        
            String message;

            Error(String message) {
                self.message = message;
            }

            String getMessage() {
                return message;
            }
        }
    }
    
    class HubConnectionOptions {
        
        bool    secure = true;
        String  connectorHost;
        int     connectorPort = null;
        String  tenant = null;
        String  key = null;
        
        HubConnectionOptions(String connectorHost, String tenant, String key) {
          self.connectorHost = connectorHost;
          self.tenant = tenant;
          self.key = key;
        }
        
        @doc("Performs Hub authentication over a secure (HTTPS) connection. By default this is true and this is mostly useful for development scenarios without TLS.")
        HubConnectionOptions setSecureAuthentication(bool value) {
          self.secure = value;
          return self;
        }
        
        bool useSecureAuthentication() {
            return secure;
        }
        
        @doc("Use a specific port for Hub authentication.")        
        HubConnectionOptions setConnectorPort(int value) {
            self.connectorPort = value;
            return self;
        }
        
        String getTenant() {
            return tenant;
        }
        
        String getKey() {
            return key;
        }
        
        String getConnectorHost() {
            return connectorHost;
        }
        
        int getConnectorPort() {
          return connectorPort;
        }
        
        String buildConnectorUrl(String path) {
            String scheme = null;
            int connectorPort = self.connectorPort;
            
            if (secure) {
              scheme = "https";
              if (connectorPort == null) {
                connectorPort = 443;
              }
            } else {
              scheme = "http";
              if (connectorPort == null) {
                connectorPort = 80;
              }
            }
            
            String result = scheme + "://" + connectorHost + ":" + connectorPort.toString();
            if (path != null) {
              result = result + path;
            }
            
            return result;
        }
    }
    
    class HubConnection extends WSHandler, HTTPHandler {

      Runtime               runtime;
      WebSocket             socket;
      HubConnectionOptions  options;
      HubHandler            handler;

      String                hubUrl;
      String                jwt;

      HubConnection(Runtime runtime, HubConnectionOptions options, HubHandler handler) {
          self.runtime = runtime;
          self.options = options;
          self.handler = handler;
      }

      bool isConnected() {
          return socket != null;
      }

      void authenticate() {
          String connectorUrl = options.buildConnectorUrl("/");
          String query = "?id=" + options.getTenant() + "&key=" + options.getKey();
          
          HTTPRequest request = new HTTPRequest(connectorUrl + query);
          request.setMethod("POST");
          self.runtime.request(request, self);
      }

      void connect() {
          self.runtime.open(hubUrl + "/?token=" + jwt, self);
      }

      void disconnect() {
          if (socket != null) {
              socket.close();
          }
      }

      void send(String data) {
          if (socket != null && isConnected()) {
              socket.send(data);
          }
      }

      void onWSConnected(WebSocket socket) {
          self.socket = socket;
          message.HubMessage msg = self.buildMessageOfType("connected", new JSONObject());
          msg.dispatch(handler);
      }

      void onWSClosed(WebSocket socket) {
          self.socket = null;
          message.HubMessage msg = self.buildMessageOfType("disconnected", new JSONObject());
          msg.dispatch(handler);
      }

      void onWSMessage(WebSocket socket, String raw) {
          JSONObject json = raw.parseJSON();
          message.HubMessage msg = self.buildMessage(json);
          msg.dispatch(handler);
      }

      void onHTTPResponse(HTTPRequest request, HTTPResponse response) {
          if (response.getCode() == 2) {
              JSONObject connectionInfo = response.getBody().parseJSON();
              hubUrl = connectionInfo["url"];
              jwt = connectionInfo["jwt"];
          } else {
              message.HubError error = new message.HubError(0, "SOME ERROR");
              error.dispatch(self.handler);
          }
      }
      
      message.HubMessage buildMessageOfType(String type, JSONObject json) {
          if (type == "connected")    { return new message.Connected(); }
          if (type == "disconnected") { return new message.Disconnected(); }
          if (type == "sync")         { return new message.Synchronize(); }
          
          return new message.HubMessage("message");      
      }

      message.HubMessage buildMessage(JSONObject json) {
          String type = json["type"].getString();
          return buildMessageOfType(type, json);
      }
    }
    
    @doc("Handler for Datawire Hub messages. Not all messages are useful to clients.")
    interface HubHandler {        
        void onClientError(message.HubError error) {
            self.onHubMessage(error);
        }    
    
        void onConnected(message.Connected connect) {
            self.onHubMessage(connect);
        }

        void onDisconnected(message.Disconnected disconnected) {
            self.onHubMessage(disconnected);
        }
        
        void onError(message.HubError error) {
            if (error.code > 999 && error.code < 2000) { self.onServerError(error); }
            if (error.code > 1999 && error.code < 3000) { self.onClientError(error); }
        }        
        
        void onHeartbeat(message.Heartbeat heartbeat) {
            self.onHubMessage(heartbeat);
        }        
        
        void onHubMessage(message.HubMessage message) {
            // generic do-nothing handler
        }
        
        void onServerError(message.HubError error) {
            self.onHubMessage(error);
        }        

        void onSubscribe(message.Subscribe sub) {
            self.onHubMessage(sub);
        }        
        
        void onSynchronize(message.Synchronize sync) {
            self.onHubMessage(sync);
        }
        
        /* !! PARTIAL UPDATES ARE NOT CURRENTLY IMPLEMENTED
        
        void onUpdate(message.Update update) {
            self.onHubMessage(update);
        }
        */
    }
    
    @doc("Default handler implementation for Datawire Hub messages")
    class DefaultHubHandler extends HubHandler { }    
    
    @doc("Base implementation of a Datawire Hub client")
    class HubClient extends Task {

        Runtime                 runtime;
        HubConnectionOptions    options;
        HubConnection           connection = null;

        HubClient(Runtime runtime, HubConnectionOptions options) {
            self.runtime = runtime;
            self.options = options;
        }

        bool isConnected() {
            if (connection != null) {
                return connection.isConnected();
            }

            return false;
        }

        @doc("Send a message to a Hub")
        void sendMessage(message.HubMessage message) {
            send(message.toJSON());
        }

        @doc("Send data to a Hub")
        void send(String data) {
            if (connection != null) {
                connection.send(data);
            }
        }

        @doc("Disconnect from the Datawire Hub")
        void disconnect() {
            if (connection != null) {
                connection.disconnect();
            }
        }

        @doc("Connect to the Datawire Hub")
        void connect(HubHandler handler) {
            /*
              Connecting to a Hub is a two step process:

              1. Contact the Hub Connector and authenticate. The connector returns a token and Hub
                 URL upon successful authentication.

              2. Use the provided Hub URL and token to establish a WebSocket connection to the Hub.
            */

            if (connection == null || connection.isConnected() == false) {
                HubConnection newConnection = new HubConnection(runtime, options, handler);
                newConnection.authenticate();
                newConnection.connect();
                if (newConnection.isConnected()) {
                    connection = newConnection;
                }
            }
        }

        @doc("Executes the task after the specified period")
        void schedule(float period) {
            runtime.schedule(self, period);
        }

        @doc("The task to run")
        void onExecute(Runtime runtime) {

        }
    }
}