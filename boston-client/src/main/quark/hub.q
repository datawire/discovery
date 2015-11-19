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

    class Sherlock extends WSHandler {
        void connect() {

        }

        void onWSMessage(WebSocket socket, String data) {
            JSONObject envelope = data.parseJSON();
        }
    }

    class Watson extends ServicePublisher, Task, WSHandler {

        Runtime         runtime;
        WebSocket       socket;

        String          serviceName;
        String          serviceEndpoint;
        Registration    registration;

        Watson(Runtime runtime, String url, String serviceName, String serviceEndpoint) {
            self.runtime = runtime;
            self.serviceName = serviceName;
            self.serviceEndpoint = serviceEndpoint;
            self.registration = Registration(serviceName, serviceEndpoint, 1000);
        }

        void connect() {

        }

        Envelope buildEnvelope() {
            Envelope envelope = new Envelope();
            envelope.agent = "watson/2.0; quark 0.0.1"; // would be nice to be able to access quark version info for putting in user agents etc.
            envelope.id = "I wish I was an UUID";
            return envelope;
        }

        void send(Envelope envelope) {
            socket.send(envelope.toJson());
        }

        void sendMessage(Message message) {
            send(buildEnvelope().addMessage(message));
        }

        void sendMessages(List<Message> messages) {
            send(buildEnvelope().addMessages(messages));
        }

        void register() {
            sendMessage(registration);
        }

        void heartbeat() {
            sendMessage(Heartbeat());
        }

        String toString() {
            return "Watson(service=" + serviceName + ", endpoint=" + serviceEndpoint + ")";
        }

        void onWSConnected(WebSocket socket) {
            self.socket = socket;
            sendMessage(registration);
        }

        void onExecute(Runtime runtime) {
            sendMessage(Heartbeat());
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

        String      serviceName;
        String      serviceEndpoint;
        int         ttl = 1000;

        Registration(String serviceName, String serviceEndpoint, int ttl) {
            super("register");
            self.serviceName = serviceName;
            self.serviceEndpoint = serviceEndpoint;
            self.ttl = ttl;
        }

        String toString() {
            return "Register(serviceName: " + serviceName + ", serviceEndpoint: " + serviceEndpoint + ", ttl: " + ttl.toString() + ")";
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json["type"] = self.type;
            json["name"] = serviceName;
            json["endpoint"] = serviceEndpoint;
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
}