package io.datawire.hub.example;


import hub.*;
import io.datawire.quark.runtime.JSONObject;

public class SherlockExample {

  public static void main(String... args) {
    io.datawire.quark.runtime.Runtime runtime = new io.datawire.quark.netty.QuarkNettyRuntime();

    String hubAddress = "ws://localhost:8080";
    final Sherlock sherlock = new Sherlock(runtime, hubAddress, new HubMessageProcessor());
    sherlock.connect();
  }

  public static class HubMessageProcessor implements OnMessage {
    @Override public void run(JSONObject json) {
      System.out.println(json.toString());
    }
  }
}
