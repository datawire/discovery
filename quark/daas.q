package daas 0.1.0;

import quark.concurrent;

namespace daas {

    @doc("The Endpoint class captures address and metadata information")
    @doc("for a server functioning as a service instance.")
    class Endpoint extends Future {
        @doc("The service name.")
        String service;
        @doc("The service version.")
        String version;
        @doc("The address from which clients can reach the server.")
        String address;
        @doc("Additional metadata associated with this service instance.")
        Map<String,Object> metadata;
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

        // Endpoints we are advertising to the disco service.
        Map<String,Endpoints> advertising = new Map<String,Endpoints>();
        // Endpoints the disco says are available.
        Map<String,Endpoints> available = new Map<String,Endpoints>();
        // Endpoints we are interested in but still awaiting info
        // about. We only allow one of these at a time.
        Map<String,Endpoint> awaiting = new Map<String,Endpoint>();

        bool started = false;
        Lock mutex = new Lock();
        DiscoHandler handler;

        Discovery(String url) {
            self.url = url;
            handler = new DiscoHandler(self);
        }

        void start() {
            mutex.acquire();
            if (!started) {
                started = true;
                handler.start();
            }
            mutex.release();
        }

        void stop() {
            mutex.acquire();
            if (started) {
                started = false;
                handler.stop();
            }
            mutex.release();
        }

        @doc("Register info about a service endpoint with the discovery service.")
        void register(Endpoint endpoint) {
            mutex.acquire();
            String service = endpoint.service;
            if (!advertising.contains(service)) {
                advertising[service] = new Endpoints();
            }
            advertising[service].endpoints.add(endpoint);
            handler.register(endpoint);
            mutex.release();
        }

        @doc("Resolve a service name into an available service endpoint.")
        Endpoint resolve(String service) {
            Endpoint result;
            mutex.acquire();
            if (available.contains(service)) {
                result = available[service].choose();
            } else {
                if (awaiting.contains(service)) {
                    result = awaiting[service];
                } else {
                    result = new Endpoint();
                    result.service = service;
                    awaiting[service] = result;
                    handler.resolve(result);
                }
            }
            mutex.release();
            return result;
        }

    }

    class DiscoHandler extends HTTPHandler, WSHandler, Task {

        Discovery disco;

        DiscoHandler(Discovery discovery) {
            disco = discovery;
        }

        void start() {
            schedule(0.0);
        }

        void stop() {
            // we've been asked to stop, so cancel the request/close
            // the web socket if it is open

            // ...
        }

        void schedule(float time) {
            Context.runtime().schedule(self, 0.0);
        }

        void register(Endpoint endpoint) {
            // trigger send of delta if we are connected, otherwise do
            // nothing because the full set of endpoints will be
            // resent when we connect/reconnect

            // ...
        }

        void resolve(Endpoint endpoint) {
            // Right now the disco protocol will notify about any
            // endpoint, so we don't need to do anything here, if we
            // wanted to change this, we'd have to track the set of
            // points we are interested in resolving and communicate
            // as this changes.
        }

        void onExecute(Runtime runtime) {
            // Do our periodic chores here, this will involve checking
            // the desired state held by disco against our actual
            // state and taking any measures necessary to address the
            // difference.

            // ...
        }

        void onWSInit(WebSocket socket) {}
        void onWSConnected(WebSocket socket) {}
        void onWSMessage(WebSocket socket, String message) {}
        void onWSBinary(WebSocket socket, Buffer message) {}
        void onWSClosed(WebSocket socket) {}
        void onWSError(WebSocket socket) {}
        void onWSFinal(WebSocket socket) {}

        void onHTTPInit(HTTPRequest request) {}
        void onHTTPResponse(HTTPRequest request, HTTPResponse response) {}
        void onHTTPError(HTTPRequest request, String message) {}
        void onHTTPFinal(HTTPRequest request) {}
        
    }

}
