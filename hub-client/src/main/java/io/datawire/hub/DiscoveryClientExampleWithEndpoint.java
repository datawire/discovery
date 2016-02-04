package io.datawire.hub;


import discovery.model.Endpoint;
import discovery.client.BasicDiscoveryClient;
import io.datawire.quark.netty.QuarkNettyRuntime;


public class DiscoveryClientExampleWithEndpoint {

  public static void main(String... args) throws Exception {
    String jsonWebToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE0NTQ1OTc0NjIsImF1ZCI6WyJkYXRhd2lyZSJdfQ==.Yq4lRyZ-6WzeDCEP1_ZUR4AwrtCViF_avraH2MTnj0k=";
    QuarkNettyRuntime runtime = QuarkNettyRuntime.getRuntime();
    runtime.setAllowSync(true);

    BasicDiscoveryClient client = new BasicDiscoveryClient(runtime, "ws://localhost:52689/messages", jsonWebToken, "foobar", Endpoint.create("http", "localhost", 5247));

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
