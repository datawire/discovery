package io.datawire.hub;


import discovery.model.Endpoint;
import discovery.model.RoutingTable;
import discovery.client.BasicDiscoveryClient;
import io.datawire.quark.netty.QuarkNettyRuntime;

import java.util.ArrayList;
import java.util.Map;

public class DiscoveryClientExample {

  public static void main(String... args) throws Exception {
    String jsonWebToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0NTQ1OTc0NjIsImF1ZCI6WyJkYXRhd2lyZSJdfQ==.Yq4lRyZ-6WzeDCEP1_ZUR4AwrtCViF_avraH2MTnj0k=";
    QuarkNettyRuntime runtime = QuarkNettyRuntime.getRuntime();
    runtime.setAllowSync(true);

    BasicDiscoveryClient client = new BasicDiscoveryClient(runtime, "ws://localhost:52689/messages", jsonWebToken, null, null);

    System.out.println("--> before connect");
    client.connect();
    while(!client.isConnected()) {
      System.out.println("Waiting to connect...");
      Thread.sleep(500);
    }

    System.out.println(String.format("--> after connect (connected: %s)", client.isConnected()));

//    client.disconnect();
//    System.out.println(String.format("--> after disconnect (connected: %s)", client.isConnected()));

    System.out.println("--> before subscribe");
    client.subscribe();

    while(true) {
      RoutingTable rt = client.getRoutingTable();
      System.out.println("Got routing table...");
      for (Map.Entry<String, ArrayList<Endpoint>> routes : rt.routes.entrySet()) {
        System.out.println(routes.getKey());
        for (Endpoint endpoint : routes.getValue()) {
          System.out.print(endpoint.toString() + ", ");
        }
      }

      Thread.sleep(1000);
    }
  }
}
