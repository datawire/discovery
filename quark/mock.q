import quark.test;

class MockEvent {

    String getType();
    List<Object> getArgs();

    String toString() {
        return getType() + getArgs().toString();
    }

}

class OpenEvent extends MockEvent {

    String url;
    WSHandler handler;
    MockSocket sock = null;
    bool closed = false;
    int expectIdx = 0;

    OpenEvent(String url, WSHandler handler) {
        self.url = url;
        self.handler = handler;
    }

    String getType() {
        return "open";
    }

    List<Object> getArgs() {
        return [url, handler];
    }

    void accept() {
        if (sock != null) {
            Context.runtime().fail("already accepted");
        } else {
            sock = new MockSocket();
            handler.onWSInit(sock);
            handler.onWSConnected(sock);
        }
    }

    void send(String message) {
        handler.onWSMessage(sock, message);
    }

    void close() {
        if (closed) {
            Context.runtime().fail("already closed");
        } else {
            handler.onWSClosed(sock);
            handler.onWSFinal(sock);
            closed = true;
        }
    }

    String expect() {
        if (sock == null) {
            Context.runtime().fail("not accepted");
        }

        if (check(expectIdx < sock.messages.size(), "expected a message")) {
            String msg = sock.messages[expectIdx];
            expectIdx = expectIdx + 1;
            return msg;
        }

        return null;
    }

}

class MockSocket extends WebSocket {

    List<String> messages = [];

    bool send(String message) {
        messages.add(message);
        return true;
    }

    bool sendBinary(Buffer bytes) {
        // ...
        return true;
    }

    bool close() {
        // ...
        return true;
    }

}

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

class MockRuntime extends Runtime {

    Runtime runtime;
    List<MockEvent> events = [];
    List<MockTask> tasks = [];
    int executed = 0;

    MockRuntime(Runtime runtime) {
        self.runtime = runtime;
    }

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
        events.add(new OpenEvent(url, handler));
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
        // ???
        runtime.serveHTTP(url, servlet);
    }
    void serveWS(String url, WSServlet servlet) {
        // ???
        runtime.serveWS(url, servlet);
    }
    void respond(HTTPRequest request, HTTPResponse response) {
        // ???
        runtime.respond(request, response);
    }

    void fail(String message) {
        runtime.fail(message);
    }
    Logger logger(String topic) {
        return runtime.logger(topic);
    }

}

class ProtocolTest {

    MockRuntime mock;
    Context old;
    int expectIdx = 0;

    void setup() {
        old = Context.current();
        Context ctx = new Context(Context.current());
        mock = new MockRuntime(ctx._runtime);
        ctx._runtime = mock;
        Context.swap(ctx);
        expectIdx = 0;
    }


    void teardown() {
        Context.swap(old);
    }

    void pump() {
        mock.pump();
    }

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

    RequestEvent expectRequest(String expectedUrl) {
        RequestEvent rev = ?expectEvent("request");
        if (rev != null) {
            String url = rev.request.getUrl();
            if (check(url == expectedUrl, "expected request event to url(" + expectedUrl + ")")) {
                return rev;
            }
        }
        return null;
    }

    OpenEvent expectOpen(String expectedUrl) {
        OpenEvent oev = ?expectEvent("open");
        if (oev != null) {
            String url = oev.url;
            if (check(url == expectedUrl, "expected open event to url(" + expectedUrl + ")")) {
                return oev;
            }
        }
        return null;
    }

}
