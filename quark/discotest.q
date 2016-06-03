quark 0.7;

package discotest 2.0.0;

use discovery-2.0.0.q;
include mock.q;

import quark.test;

import discovery;
import discovery.protocol;
import mock;

void main(List<String> args) {
    test.run(args);
}

class DiscoTest extends ProtocolTest {

    /////////////////
    // Helpers

    DiscoveryEvent expectDiscoveryEvent(SocketEvent sev, String expectedType) {
        TextMessage msg = sev.expectTextMessage();
        if (msg == null) { return null; }
        DiscoveryEvent evt = DiscoveryEvent.decode(msg.text);
        String type = evt.getClass().getName();
        if (check(type == expectedType, "expected " + expectedType + " event, got " + type)) {
            return ?evt;
        } else {
            return null;
        }
    }

    Open expectOpen(SocketEvent evt) {
        return ?expectDiscoveryEvent(evt, "discovery.protocol.Open");
    }

    Active expectActive(SocketEvent evt) {
        return ?expectDiscoveryEvent(evt, "discovery.protocol.Active");
    }

    void checkEqualNodes(Node expected, Node actual) {
        checkEqual(expected.service, actual.service);
        checkEqual(expected.address, actual.address);
        checkEqual(expected.version, actual.version);
        checkEqual(expected.properties, actual.properties);
    }

    SocketEvent startDisco(Discovery disco) {
        disco.start();
        self.pump();
        RequestEvent rev = self.expectRequest(disco.url + "/v2/connect");
        if (rev == null) { return null; }
        rev.respond(200, {}, "ws://discoball");
        self.pump();
        SocketEvent sev = self.expectSocket("ws://discoball");
        if (sev == null) { return null; }
        sev.accept();
        return sev;
    }

    /////////////////
    // Tests

    void testStart() {
        Discovery disco = new Discovery("http://gateway");

        // we should see no events until we tell disco to start
        self.expectNone();
        disco.start();

        // now lets tell our mock runtime to pump
        self.pump();
        RequestEvent rev = self.expectRequest("http://gateway/v2/connect");
        if (rev == null) { return; }
        if (disco.token != null) {
            String token = rev.request.getHeader("Authorization");
            checkEqual("Bearer " + disco.token, token);
        }
        rev.respond(200, {}, "ws://discoball");
        self.pump();
        SocketEvent sev = self.expectSocket("ws://discoball");
        if (sev == null) { return; }
    }

    void testFailedStart() {
        // ...
    }

    void testRegisterPreStart() {
        Discovery disco = new Discovery("http://gateway");

        Node node = new Node();
        node.service = "svc";
        node.address = "addr";
        node.version = "1.2.3";
        disco.register(node);

        SocketEvent sev = startDisco(disco);
        if (sev == null) { return; }

        Open open = expectOpen(sev);
        if (open == null) { return; }

        Active active = expectActive(sev);
        if (active == null) { return; }
        checkEqualNodes(node, active.node);
    }

    void testRegisterPostStart() {
        Discovery disco = new Discovery("http://gateway");
        SocketEvent sev = startDisco(disco);

        Node node = new Node();
        node.service = "svc";
        node.address = "addr";
        node.version = "1.2.3";
        disco.register(node);

        Open open = expectOpen(sev);
        if (open == null) { return; }
        Active active = expectActive(sev);
        if (active == null) { return; }
        checkEqualNodes(node, active.node);
    }

    void testResolvePreStart() {
        Discovery disco = new Discovery("http://discoball");

        Node node = disco.resolve("svc");
        checkEqual("svc", node.service);
        checkEqual(null, node.address);
        checkEqual(null, node.version);
        checkEqual(null, node.properties);

        SocketEvent sev = startDisco(disco);
        if (sev == null) { return; }

        Active active = new Active();
        active.node = new Node();
        active.node.service = "svc";
        active.node.address = "addr";
        active.node.version = "1.2.3";
        sev.send(active.encode());

        checkEqualNodes(active.node, node);
    }

    void testResolvePostStart() {
        Discovery disco = new Discovery("http://discoball");
        SocketEvent sev = startDisco(disco);

        Node node = disco.resolve("svc");
        checkEqual("svc", node.service);
        checkEqual(null, node.address);
        checkEqual(null, node.version);
        checkEqual(null, node.properties);

        Active active = new Active();
        active.node = new Node();
        active.node.service = "svc";
        active.node.address = "addr";
        active.node.version = "1.2.3";
        sev.send(active.encode());

        checkEqualNodes(active.node, node);
    }

    void testLoadBalancing() {
        Discovery disco = new Discovery("http://discoball");
        SocketEvent sev = startDisco(disco);

        Node node = disco.resolve("svc");
        checkEqual("svc", node.service);
        checkEqual(null, node.address);
        checkEqual(null, node.version);
        checkEqual(null, node.properties);

        Active active = new Active();

        int idx = 0;
        int count = 10;
        while (idx < count) {
            active.node = new Node();
            active.node.service = "svc";
            active.node.address = "addr" + idx.toString();
            active.node.version = "1.2.3";
            sev.send(active.encode());
            idx = idx + 1;
        }

        idx = 0;
        while (idx < count*10) {
            node = disco.resolve("svc");
            checkEqual("addr" + (idx % count).toString(), node.address);
            idx = idx + 1;
        }
    }

    void testReconnect() {
        // ...
    }

    void testStop() {
        // ...
    }

}
