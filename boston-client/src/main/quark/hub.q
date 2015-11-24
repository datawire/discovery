@version("0.0.1")
@doc("")
package hub {

    /*
        User defined primitives, for example, UUID generation.
    @mapping(
        $java(java.util.UUID)
        $py()
        $js()
    )
    primitive UUID {

    }
    */

    interface ServicePublisher {
        void register();
        void heartbeat();
    }

    @doc("Represents the port associated with a service endpoint and the protocol it handles")
    class ServicePort {

      String name;
      int port;

      ServicePort(String name, int port) {
        self.name = name;
        self.port = port;
      }

      String toString() {
        return name + "-" + port.toString();
      }

      JSONObject toJson() {
        JSONObject json = new JSONObject();
        json["name"] = name;
        json["port"] = port;
        json["secure"] = false;
        return json;
      }
    }

    @doc("Represents the IP or DNS name associated with a service endpoint")
    class NetworkAddress {

      String host;
      String type;

      NetworkAddress(String host, String type) {
        self.host = host;
        self.type = type;
      }

      JSONObject toJson() {
        JSONObject json = new JSONObject();
        json["host"] = host;
        json["type"] = type;
        return json;
      }
    }

    class ServiceEndpoint {

      NetworkAddress address;
      ServicePort port;

      ServiceEndpoint(NetworkAddress address, ServicePort port) {
        self.address = address;
        self.port = port;
      }

      String toString() {
        return "";
      }

      JSONObject toJson() {
        JSONObject json = new JSONObject();
        json["address"] = address.toJson();
        json["port"] = port.toJson();
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

    class HubClient extends WSHandler {

        Runtime runtime;
        WebSocket socket;
        String hubAddress;

        HubClient(Runtime runtime, String hubAddress) {
          self.runtime = runtime;
          self.hubAddress = hubAddress;
        }

        Envelope buildEnvelope() {
            Envelope envelope = new Envelope();
            envelope.agent = "watson/2.0; quark 0.0.1"; // would be nice to be able to access quark version info for putting in user agents etc.
            return envelope;
        }

        void connect() {
          self.runtime.open(self.hubAddress, self);
        }

        void disconnect() {
          sendMessage(Disconnect());
        }

        void onWSConnected(WebSocket socket) {
            self.socket = socket;
        }

        void onWSClosed(WebSocket socket) {
            self.socket = null;
        }

        void send(Envelope envelope) {
            socket.send(envelope.toJson().toString());
        }

        void sendMessage(Message message) {
            send(buildEnvelope().addMessage(message));
        }

        void sendMessages(List<Message> messages) {
            send(buildEnvelope().addMessages(messages));
        }
    }

    class Sherlock extends HubClient, WSHandler {

        OnMessage callback;

        Sherlock(Runtime runtime, String hubAddress, OnMessage callback) {
          super(runtime, hubAddress);
          self.callback = callback;
          //self.runtime.open(hubAddress, self);
          //self.connect();
        }

        void onWSConnected(WebSocket socket) {
          self.socket = socket;
          super.sendMessage(new Subscription());
        }

        void onWSMessage(WebSocket socket, String data) {
            JSONObject envelope = data.parseJSON();
            self.callback.run(envelope);
        }
    }

    interface OnMessage {
      void run(JSONObject json);
    }

    interface HealthCheck {
      bool check();
    }

    class Watson extends HubClient, ServicePublisher, Task, WSHandler {

        String          serviceName;
        ServiceEndpoint serviceEndpoint;
        Registration    registration;
        HealthCheck     healthCheck;
        bool            firstCheck = true;

        Watson(Runtime runtime, String hubAddress, String serviceName, ServiceEndpoint serviceEndpoint) {
            super(runtime, hubAddress);
            self.serviceName = serviceName;
            self.serviceEndpoint = serviceEndpoint;
            self.registration = Registration(serviceName, serviceEndpoint, 1000);
            //self.connect()
            //self.runtime.open(self.hubAddress, self);
        }

        void registerHealthCheck(HealthCheck healthCheck) {
            print("registered health check");
            self.healthCheck = healthCheck;
        }

        void register() {
            while(self.socket == null) {
              print(".")
            }

            super.sendMessage(registration);
            self.runtime.schedule(self, 5.0);
        }

        void heartbeat() {
            super.sendMessage(Heartbeat());
        }

        String toString() {
            return "Watson(service=" + serviceName + ", endpoint=" + serviceEndpoint.toString() + ")";
        }

        void onWSConnected(WebSocket socket) {
            self.socket = socket;
            super.sendMessage(registration);
        }

        void onExecute(Runtime runtime) {
            if (healthCheck.check()) {
              // Alive
              if (self.socket == null) {
                print("DEAD -> LIVE " + serviceEndpoint.toString());
                self.connect();
                self.heartbeat();
              }
            } else {
              if (self.socket != null) {
                print("LIVE -> DEAD " + serviceEndpoint.toString());
                self.disconnect();
              } else {
                if (firstCheck) {
                  print("START -> DEAD" + serviceEndpoint.toString());
                  firstCheck = false;
                }
              }
            }

            self.runtime.schedule(self, 5.0);
        }
    }

    class Envelope {

        String          agent = null;
        String          id = null;
        List<Message>   messages = [];

        Envelope addMessage(Message message) {
            messages.add(message);
            return self;
        }

        Envelope addMessages(List<Message> messages) {
            int idx = 0;
            while(idx < messages.size()) {
                self.messages.add(messages[idx]);
                idx = idx + 1;
            }

            return self;
        }

        String toString() {
            return "Envelope(agent: " + agent + ", messages: " + messages.size().toString() + ")";
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json["agent"] = agent;

            JSONObject messagesArray = new JSONObject().setList();
            int idx = 0;
            while(idx < messages.size()) {
                messagesArray.setListItem(idx, messages[idx].toJson());
                idx = idx + 1;
            }

            json["messages"] = messagesArray;
            return json;
        }
    }

    class Message {

        String      type;

        Message(String type) {
            if (type == null) {
                // todo: Exception!
            }

            self.type = type;
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json["type"] = self.type;
            return json;
        }
    }

    class Heartbeat extends Message {
        Heartbeat() {
            super("heartbeat");
        }

        String toString() {
            return "Heartbeat()";
        }
    }

    class Registration extends Message {

        String          serviceName;
        ServiceEndpoint serviceEndpoint;
        int             ttl = 1000;

        Registration(String serviceName, ServiceEndpoint serviceEndpoint, int ttl) {
            super("register");
            self.serviceName = serviceName;
            self.serviceEndpoint = serviceEndpoint;
            self.ttl = ttl;
        }

        String toString() {
            return "Register(serviceName: " + serviceName + ", serviceEndpoint: " + serviceEndpoint.toString() + ", ttl: " + ttl.toString() + ")";
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json["type"] = self.type;
            json["name"] = serviceName;
            json["endpoint"] = serviceEndpoint.toJson();
            return json;
        }
    }

    class Subscription extends Message {

        Subscription() {
            super("subscribe");
        }

        String toString() {
            return "Subscription()";
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json["type"] = self.type;
            return json;
        }
    }

    class Disconnect extends Message {

        Disconnect() {
            super("disco");
        }

        String toString() {
            return "Disconnect()";
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json["type"] = self.type;
            return json;
        }
    }
}