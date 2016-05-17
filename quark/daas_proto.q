import daas;

namespace daas {
  namespace proto {

    @doc("The protocol machinery that wires together the public disco API to a server.")
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
        Context.runtime().schedule(self, time);
      }

      void register(Endpoint endpoint) {
        // Trigger send of delta if we are connected, otherwise do
        // nothing because the full set of endpoints will be
        // resent when we connect/reconnect.

        // ...
      }

      void resolve(ServiceInfo endpoint) {
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

        if (!disco.services.contains(service)) {
          disco.services[service] = new ServiceInfo();
        }

        List<Endpoint> endpoints = disco.services[service].endpoints;

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

        if (disco.services.contains(service)) {
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
        // Any non-transient errors should be reported back to the
        // user via any Endpoints they have requested.
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

        // Any non-transient errors should be reported back to the
        // user via any Endpoints they have requested.
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

      String encode() {
        JSONObject json = toJSON(self, self.getClass());
        return json.toString();
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
}
