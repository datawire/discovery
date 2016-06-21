quark 1.0;

import quark.test;

// THIS CAN BE REPLACED WITH quark.mock AS SOON AS WE UPGRADE TO QUARK > 1.0.133!
// DELETE THIS FILE!

/*@doc("""
Base class for network actions that have been captured by the
MockRuntime.
""")*/
class MockEvent {

    String getType();
    List<Object> getArgs();

    String toString() {
        return getType() + getArgs().toString();
    }

}

/*@doc("""
A SocketEvent indicates that a web socket open has been initiated. The
SocketEvent class can be used to inspect the url and simulate remote
interactions with the socket.
""")*/
class SocketEvent extends MockEvent {

    String url;
    WSHandler handler;
    MockSocket sock = null;
    bool closed = false;
    int expectIdx = 0;

    SocketEvent(String url, WSHandler handler) {
        self.url = url;
        self.handler = handler;
    }

    String getType() {
        return "socket";
    }

    List<Object> getArgs() {
        return [url, handler];
    }

    /*@doc("""
    Simulate the remote peer accepting the socket connect.
    """)*/
    void accept() {
        if (sock != null) {
            Context.runtime().fail("already accepted");
        } else {
            sock = new MockSocket();
            handler.onWSInit(sock);
            handler.onWSConnected(sock);
        }
    }

    /*@doc("""
    Simulate the remote peer sending a text message.
    """)*/
    void send(String message) {
        handler.onWSMessage(sock, message);
    }

    /*@doc("""
    Simulate the remote peer closing the socket.
    """)*/
    void close() {
        if (closed) {
            Context.runtime().fail("already closed");
        } else {
            handler.onWSClosed(sock);
            handler.onWSFinal(sock);
            closed = true;
        }
    }

    /*@doc("""
    Check that a message has been sent on the socket associated with
    this socket event. The message is returned.
    """)*/
    MockMessage expectMessage() {
        if (sock == null) {
            Context.runtime().fail("not accepted");
        }

        if (check(expectIdx < sock.messages.size(), "expected a message")) {
            MockMessage msg = sock.messages[expectIdx];
            expectIdx = expectIdx + 1;
            return msg;
        }

        return null;
    }

    /*@doc("""
    Check that a text message has been sent on the socket associated with
    this socket event. The text message is returned.
    """)*/
    TextMessage expectTextMessage() {
        MockMessage msg = expectMessage();
        if (msg != null && msg.isText()) {
            return ?msg;
        } else {
            return null;
        }
    }

    /*@doc("""
    Check that a binary message has been sent on the socket associated with
    this socket event. The binary message is returned.
    """)*/
    BinaryMessage expectBinaryMessage() {
        MockMessage msg = expectMessage();
        if (msg != null && msg.isBinary()) {
            return ?msg;
        } else {
            return null;
        }
    }

}

class MockMessage {
    bool isBinary() {
        return !isText();
    }
    bool isText();
}

@doc("A text message.")
class TextMessage extends MockMessage {

    @doc("The message content.")
    String text;

    TextMessage(String message) {
        text = message;
    }

    bool isText() {
        return true;
    }
}

@doc("A binary message.")
class BinaryMessage extends MockMessage {

    @doc("The message content.")
    Buffer bytes;

    BinaryMessage(Buffer message) {
        // XXX: we should probably copy this, but no convenient copy for Buffer right now
        bytes = message;
    }

    bool isText() {
        return false;
    }
}

class MockSocket extends WebSocket {

    List<MockMessage> messages = [];

    bool send(String message) {
        messages.add(new TextMessage(message));
        return true;
    }

    bool sendBinary(Buffer bytes) {
        messages.add(new BinaryMessage(bytes));
        return true;
    }

    bool close() {
        // ...
        return true;
    }

}

/*@doc("""
A RequestEvent indicates that an HTTPRequest has been initiated. This
class can be used to examine the request and mock a response.
""")*/
class RequestEvent extends MockEvent {

    HTTPRequest request;
    HTTPHandler handler;

    RequestEvent(HTTPRequest request, HTTPHandler handler) {
        self.request = request;
        self.handler = handler;
    }

    String getType() {
        return "request";
    }

    List<Object> getArgs() {
        return [request, handler];
    }

    /*@doc("""
    Supply a mock response for the request captured in this RequestEvent.
    """)*/
    void respond(int code, Map<String,String> headers, String body) {
        MockResponse response = new MockResponse();
        response.code = code;
        response.headers = headers;
        response.body = body;
        handler.onHTTPInit(request);
        handler.onHTTPResponse(request, response);
        handler.onHTTPFinal(request);
    }

}

class MockResponse extends HTTPResponse {

    int code;
    String body;
    Map<String,String> headers = {};

    int getCode() { return code; }
    void setCode(int code) { self.code = code; }
    String getBody() { return body; }
    void setBody(String body) { self.body = body; }
    void setHeader(String key, String value) { headers[key] = value; }
    String getHeader(String key) { return headers[key]; }
    List<String> getHeaders() { return headers.keys(); }

}

class MockTask {

    Task task;
    float delay;

    MockTask(Task task, float delay) {
        self.task = task;
        self.delay = delay;
    }
}

/*@doc("""
The MockRuntime is intended for testing protocol endpoints written in
quark. When enabled as the current runtime, the MockRuntime will
capture any endpoint initiated network actions such as making http
requests, opening a web socket, sending a message on a web socket,
etc. In addition, the mock runtime captures scheduled tasks and allows
them to be executed in a controlled manner.

This allows a normal unit testing framework to drive the API for a
protocol endpoint, examine all network interactions that occur, mock
the responses, and step through asynchronous tasks in a controlled
manner.
""")*/
class MockRuntime extends Runtime {

    Runtime runtime;
    List<MockEvent> events = [];
    List<MockTask> tasks = [];
    int executed = 0;

    MockRuntime(Runtime runtime) {
        self.runtime = runtime;
    }

    @doc("Execute all currently scheduled tasks.")
    void pump() {
        // snapshot the size so that respawning tasks don't loop forever
        int size = tasks.size();
        while (executed < size) {
            Task next = tasks[executed].task;
            // XXX: we need to be able to intercept queries of the clock
            // too so that we can accelerate the clock by the appropriate
            // amount when we execute tasks
            next.onExecute(self);
            executed = executed + 1;
        }
    }

    void open(String url, WSHandler handler) {
        events.add(new SocketEvent(url, handler));
    }

    void request(HTTPRequest request, HTTPHandler handler) {
        events.add(new RequestEvent(request, handler));
    }

    void schedule(Task handler, float delayInSeconds) {
        tasks.add(new MockTask(handler, delayInSeconds));
    }

    Codec codec() {
        return runtime.codec();
    }

    void serveHTTP(String url, HTTPServlet servlet) {
        runtime.fail("Runtime.serveHTTP not yet supported by the MockRuntime");
    }

    void serveWS(String url, WSServlet servlet) {
        runtime.fail("Runtime.serveWS not yet supported by the MockRuntime");
    }

    void respond(HTTPRequest request, HTTPResponse response) {
        runtime.fail("Runtime.respond not yet supported by the MockRuntime");
    }

    void fail(String message) {
        runtime.fail(message);
    }

    Logger logger(String topic) {
        return runtime.logger(topic);
    }

}

/*@doc("""
The ProtocolTest class provides a base test case that automatically
installs/uninstall a MockRuntime in setup/teardown respectively. It
also provides a basic expect-stlye API to check that expected events
have actually ocurred, and returns those events for inspection, as
well as for use in mocking fake responses.
""")*/
class ProtocolTest {

    MockRuntime mock;
    Context old;
    int expectIdx = 0;
    Map<String,SocketEvent> sockets;

    void setup() {
        old = Context.current();
        Context ctx = new Context(Context.current());
        mock = new MockRuntime(ctx._runtime);
        ctx._runtime = mock;
        Context.swap(ctx);
        expectIdx = 0;
        sockets = {};
    }


    void teardown() {
        Context.swap(old);
    }

    @doc("Execute any pending asynchronous tasks.")
    void pump() {
        mock.pump();
    }

    /*@doc("""
    Checks that there are no captured I/O events. The number of actual
    I/O events are returned.
    """)*/
    int expectNone() {
        int delta = mock.events.size() - expectIdx;
        check(delta == 0, "expected no events, got " + delta.toString());
        return delta;
    }

    MockEvent expectEvent(String expectedType) {
        MockEvent result = null;
        if (check(mock.events.size() > expectIdx, "expected " + expectedType + " event, got no events")) {
            String type = mock.events[expectIdx].getType();
            if (check(type == expectedType, "expected " + expectedType + " event, got " + type)) {
                result = mock.events[expectIdx];
            }
        }
        expectIdx = expectIdx + 1;
        return result;
    }

    /*@doc("""
    Check that the next logged event is a RequestEvent to the expected
    URL. Passing in a null will match any URL. If a matching event is
    found it is returned, otherwise null is returned.
    """)*/
    RequestEvent expectRequest(String expectedUrl) {
        RequestEvent rev = ?expectEvent("request");
        if (rev != null) {
            if (expectedUrl == null) { return rev; }
            String url = rev.request.getUrl();
            if (check(url == expectedUrl, "expected request event to url(" + expectedUrl + "), got url("
                      + url + ")")) {
                return rev;
            }
        }
        return null;
    }

    /*@doc("""
    Check that the next logged event is a SocketEvent with the expected
    URL. Passing in a null will match any URL. If a matching event is
    found it is returned, otherwise null is returned.
    """)*/
    SocketEvent expectSocket(String expectedUrl) {
        SocketEvent sev = ?expectEvent("socket");
        if (sev != null) {
            sockets[sev.url] = sev;
            if (expectedUrl == null) { return sev; }
            String url = sev.url;
            if (check(url == expectedUrl, "expected socket event to url(" + expectedUrl + "), got url(" +
                      url + ")")) {
                return sev;
            }
        }
        return null;
    }

}
