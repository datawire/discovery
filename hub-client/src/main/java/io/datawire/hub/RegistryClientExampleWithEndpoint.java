package io.datawire.hub;


import hub.model.Endpoint;
import hub.model.RoutingTable;
import hub.registry.BasicRegistryClient;
import io.datawire.quark.netty.QuarkNettyRuntime;

import java.util.ArrayList;
import java.util.Map;

public class RegistryClientExampleWithEndpoint {

  public static void main(String... args) throws Exception {
    String jsonWebToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0NTQ1OTc0NjIsImF1ZCI6WyJkYXRhd2lyZSJdfQ==.Yq4lRyZ-6WzeDCEP1_ZUR4AwrtCViF_avraH2MTnj0k=";
    QuarkNettyRuntime runtime = QuarkNettyRuntime.getRuntime();
    runtime.setAllowSync(true);

    BasicRegistryClient client = new BasicRegistryClient(runtime, "ws://localhost:52689/messages", jsonWebToken, "foobar", Endpoint.create("http", "localhost", 5247));

    System.out.println("--> before connect");
    client.connect();
    while(!client.isConnected()) {
      System.out.println("Waiting to connect...");
      Thread.sleep(500);
    }

    System.out.println(String.format("--> after connect (connected: %s)", client.isConnected()));

    System.out.println("--> before subscribe");
    client.registerEndpoint();
  }
}
