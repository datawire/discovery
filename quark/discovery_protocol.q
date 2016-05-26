import discovery;

namespace discovery {
namespace protocol {

    @doc("The protocol machinery that wires together the public disco API to a server.")
    class DiscoClient extends DiscoHandler, HTTPHandler, WSHandler, Task {

        Discovery disco;
	float restartDelay = 0.1;
        WebSocket sock = null;
        bool authenticating = false;

        DiscoClient(Discovery discovery) {
            disco = discovery;
        }

        bool isConnected() {
            return sock != null;
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

        void register(Node node) {
            // Trigger send of delta if we are connected, otherwise do
            // nothing because the full set of nodes will be
            // resent when we connect/reconnect.

            // ...
        }

        void resolve(Node node) {
            // Right now the disco protocol will notify about any
            // node, so we don't need to do anything here, if we
            // wanted to change this, we'd have to track the set of
            // points we are interested in resolving and communicate
            // as this changes.

	    // Hmm, maybe we actually need to do something to deal
	    // with timeouts.
        }

	void onActive(Active active) {
	    // Stick the node in the available set.

	    Node node = active.node;
	    String service = node.service;

	    if (!disco.services.contains(service)) {
		disco.services[service] = new Cluster();
	    }

            Cluster cluster = disco.services[service];
            cluster.add(node);
	}

	void onExpire(Expire expire) {
	    // Remove the node from our available set.

	    // hmm, we could make all Node objects we hand out be
	    // continually updated until they expire...

	    Node node = expire.node;
	    String service = node.service;

	    if (disco.services.contains(service)) {
		// XXX: no way to remove from List or Map
		// ...
	    }

	    // ...
	}

        void onClear(Clear reset) {
            // ???
        }

        void onExecute(Runtime runtime) {
            /*
              Do our periodic chores here, this will involve checking
              the desired state held by disco against our actual
              state and taking any measures necessary to address the
              difference:
	      
              
               - Disco.started holds the desired connectedness
                 state. The isConnected() accessor holds the actual
                 connectedness state. If these differ then do what is
                 necessry to make the desired state actual.
              
	       - If we haven't sent a heartbeat recently enough, then
                 do that.
            */

            if (isConnected()) {
                if (!disco.started) {
                    sock.close();
                    sock = null;
                }
            } else {
                if (!authenticating) {
                    authRequest();
                    authenticating = true;
                }
            }
        }

        void authRequest() {
            HTTPRequest request = new HTTPRequest(disco.url + "/v2/connect");
            request.setMethod("POST");
            if (disco.token != null) {
                request.setHeader("Authorization", "Bearer " + disco.token);
            }
            Context.runtime().request(request, self);
        }

        void onHTTPInit(HTTPRequest request) { /* unused */ }
        void onHTTPResponse(HTTPRequest request, HTTPResponse response) {
            if (response.getCode() == 200) {
                String url = response.getBody();
                Context.runtime().open(url, self);
            } else {
                // ...
            }
	}
        void onHTTPError(HTTPRequest request, String message) {
	    // Any non-transient errors should be reported back to the
	    // user via any Nodes they have requested.
	}
        void onHTTPFinal(HTTPRequest request) { /* unused */ }

        void onWSInit(WebSocket socket) { /* unused */ }
        void onWSConnected(WebSocket socket) {
	    // Whenever we (re)connect, notify the server of any
	    // nodes we have registered.

	    // ...
	}
        void onWSMessage(WebSocket socket, String message) {
	    // Decode and dispatch incoming messages.
	    DiscoveryEvent event = DiscoveryEvent.decode(message);
	    disco.mutex.acquire();
	    event.dispatch(self);
	    disco.mutex.release();
	}
        void onWSBinary(WebSocket socket, Buffer message) { /* unused */ }

        void onWSClosed(WebSocket socket) { /* unused */ }

        void onWSError(WebSocket socket) {
	    // XXX: Should log the error here.

            // Any non-transient errors should be reported back to the
            // user via any Nodes they have requested.
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

        void onClear(Clear reset);

    }

    class DiscoveryEvent {

	static DiscoveryEvent decode(String message) {
	    JSONObject json = message.parseJSON();
	    String type = json["type"];
	    Class clazz = Class.get(type);
	    DiscoveryEvent event = ?clazz.construct([]);
	    fromJSON(clazz, event, json);
	    return event;
	}

        String encode() {
            Class clazz = self.getClass();
            JSONObject json = toJSON(self, clazz);
            json["type"] = clazz.getName();
            return json.toString();
        }

	void dispatch(DiscoHandler handler);

    }

    /*@doc("""
    Advertise a node as being active. This can be used to register a
    new node or to heartbeat an existing node. The receiver must
    consider the node to be available for the duration of the
    specified ttl.
    """)*/
    class Active extends DiscoveryEvent {

        @doc("The advertised node.")
	Node node;
        @doc("The ttl of the node in seconds.")
        float ttl;

	void dispatch(DiscoHandler handler) {
	    handler.onActive(self);
	}
    }

    @doc("Expire a node.")
    class Expire extends DiscoveryEvent {

	Node node;

	void dispatch(DiscoHandler handler) {
	    handler.onExpire(self);
	}
    }

    @doc("Expire all nodes.")
    class Clear extends DiscoveryEvent {
        void dispatch(DiscoHandler handler) {
            handler.onClear(self);
        }
    }

}}
