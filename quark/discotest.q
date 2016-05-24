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

    DiscoveryEvent expectDiscoveryEvent(OpenEvent oev, String expectedType) {
        String msg = oev.expect();
        if (msg == null) { return null; }
        DiscoveryEvent evt = DiscoveryEvent.decode(msg);
        String type = evt.getClass().getName();
        if (check(type == expectedType, "expected " + expectedType + " event, got " + type)) {
            return ?evt;
        } else {
            return null;
        }
    }

    Active expectActive(OpenEvent evt) {
        return ?expectDiscoveryEvent(evt, "discovery.protocol.Active");
    }

    void testStart() {
        Discovery disco = new Discovery("http://discoball");

        // we should see no events until we tell disco to start
        self.expectNone();
        disco.start();

        // now lets tell our mock runtime to pump
        self.pump();
        RequestEvent rev = self.expectRequest("http://discoball/v2/connect");
        if (rev == null) { return; }
        if (disco.token != null) {
            String token = rev.request.getHeader("Authorization");
            checkEqual("Bearer " + disco.token, token);
        }
        rev.respond(200, {}, "ws://blah");
        self.pump();
        OpenEvent oev = self.expectOpen("ws://blah");
        if (oev == null) { return; }

        // ...

        oev.accept();
        Active active = new Active();

        active.node = new Node();
        active.node.service = "svc";
        active.node.address = "addr0";
        active.node.version = "1.2.3";

        oev.send(active.encode());
        print(disco.resolve("svc").toString());

        Node node = new Node();
        node.service = "provided";
        node.address = "me";
        node.version = "3.2.1";
        disco.register(node);

        Active aev = expectActive(oev);
        if (aev == null) { return; }
        checkEqual("provided", aev.node.service);
        checkEqual("me", aev.node.address);
        checkEqual("3.2.1", aev.node.version);
        checkEqual(null, aev.node.properties);
    }

    void testResolve() {
        Discovery disco = new Discovery("http://discoball");
        Node node = disco.resolve("svc");

        checkEqual("svc", node.service);
        checkEqual(null, node.address);
        checkEqual(null, node.version);
        checkEqual(null, node.properties);
        //print(node.toString());

        Active active = new Active();

        active.node = new Node();
        active.node.service = "svc";
        active.node.address = "addr0";
        active.node.version = "1.2.3";
        active.dispatch(disco.client);

        checkEqual("svc", node.service);
        checkEqual("addr0", node.address);
        checkEqual("1.2.3", node.version);
        checkEqual(null, node.properties);
        //print(node.toString());
    }

    void testLoadBalancing() {
        Discovery disco = new Discovery("http://discoball");
        Node node = disco.resolve("svc");

        checkEqual("svc", node.service);
        checkEqual(null, node.address);
        checkEqual(null, node.version);
        checkEqual(null, node.properties);
        //print(node.toString());

        Active active = new Active();

        int idx = 0;
        int count = 10;
        while (idx < count) {
            active.node = new Node();
            active.node.service = "svc";
            active.node.address = "addr" + idx.toString();
            active.node.version = "1.2.3";
            active.dispatch(disco.client);
            idx = idx + 1;
        }

        idx = 0;
        while (idx < count*10) {
            node = disco.resolve("svc");
            checkEqual("addr" + (idx % count).toString(), node.address);
            //print(node.toString());
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
