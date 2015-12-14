package io.datawire.hub.example;


import hub.*;
import hub.event.RegistryUpdate;
import io.datawire.quark.runtime.Runtime;

public class SherlockExample {

  static class Sherlock extends RegistryClient implements Runnable {

    Sherlock(Runtime runtime) {
      super(runtime, "localhost", 1234);
    }

    @Override public void run() {
      onExecute(runtime);
    }

    @Override public void onExecute(io.datawire.quark.runtime.Runtime runtime) {
      System.out.println("beep boop");
      runtime.schedule(this, 2.0);
    }
  }

  public static void main(String... args) {
    io.datawire.quark.runtime.Runtime runtime = new io.datawire.quark.netty.QuarkNettyRuntime();

    Sherlock sherlock = new Sherlock(runtime);
    sherlock.run();

//    RegistrySubscription rs = new RegistrySubscription(new SherlockHandler(), "localhost", 1234);
//    rs.subscribe("localhost", 1234);

//    String hubAddress = "ws://localhost:1234";
//    final Sherlock sherlock = new Sherlock(runtime, hubAddress, new HubMessageProcessor());
//    sherlock.connect();
  }

  public static class SherlockHandler extends DefaultRegistryHandler {
    @Override public void onRegistryUpdate(RegistryUpdate update) {

    }
  }

//  public static class HubMessageProcessor implements OnMessage {
//    @Override public void run(JSONObject json) {
//      System.out.println(json.toString());
//    }
//  }
}
