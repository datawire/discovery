use ./quark-improvements.q;
import quark_ext1.nice;
import quark.test;


@doc("Wait for a future to fire all of it's onFuture callbacks")
void synchronize(Future f) {
    Future done = new Future();
    done.when(f);
    new concurrent.FutureWait().wait(done, 1.0);
}

class StupidClass extends Bindable {
    String id;
    StupidClass(String id) {
        self.id = id;
    }
    String callme(String foo, String bar) {
        return self.id + ".callme with "+foo+" and "+bar+"";
    }
}

class Canary extends Bindable {
    String _log;
    Canary(String init) {
        self._log = init;
    }
    void tweet(Future f, String what) {
        self.doLog(f, "tweet", what);
    }
    void chirp(Future f, String what) {
        self.doLog(f, "chirp", what);
    }
    String log() {
        return _log;
    }
    void doLog(Future f, String verb, String what) {
        if (f.isFinished()) {
            if (f.getError() != null) {
                verb = verb + "!";
            } else {
                verb = verb + ".";
            }
        } else {
            verb = verb + "?";
        }
        self._log = self._log + " " + verb + " " + what;
    }
}

class MethodTest {
    void testBound() {
        StupidClass x = new StupidClass("x");
        BoundMethod b = x.__method__("callme");
        checkEqual("x.callme with foo and bar", b.invoke(["foo", "bar"]));
    }

    void testApplied() {
        StupidClass x = new StupidClass("x");
        BoundMethod b = x.__method__("callme");
        AppliedMethod a = b.apply(["foo"]);
        checkEqual("x.callme with foo and bar", a.invoke(["bar"]));
    }

    void testFutureSuccess() {
        Future f = new Future();
        Canary c = new Canary("biwd");
        c.chirp(f, "urk");
        f.then(c.__method__("tweet"), ["before"]);
        checkEqual("biwd chirp? urk", c.log());
        f.finish(null);
        synchronize(f);
        checkEqual("biwd chirp? urk tweet. before", c.log());
        f.then(c.__method__("chirp"), ["after"]);
        synchronize(f);
        checkEqual("biwd chirp? urk tweet. before chirp. after", c.log());
    }
    void testJoin() {
        Future fa = new Future();
        Future fb = new Future();
        Future f = new Future();
        f.when(new examples.Join([fa, fb]));
        Canary c = new Canary("biwd");
        c.chirp(f, "urk");
        f.then(c.__method__("tweet"), ["before"]);
        checkEqual("biwd chirp? urk", c.log());
        fa.finish(null);
        synchronize(fa);
        checkEqual("biwd chirp? urk", c.log());
        fb.finish(null);
        f.then(c.__method__("chirp"), ["after"]);
        synchronize(f);
        checkEqual("biwd chirp? urk tweet. before chirp. after", c.log());
    }
}


void main(List<String> args) {
    test.run(args);
}
