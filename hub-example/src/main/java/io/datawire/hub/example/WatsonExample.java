package io.datawire.hub.example;


import hub.*;
import io.datawire.quark.runtime.Runtime;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Random;

public class  WatsonExample {

  public static void main(String... args) {
    Runtime runtime = new io.datawire.quark.netty.QuarkNettyRuntime();

    String hubAddress = "localhost:8080";
    String serviceName = "foobar-service";
    ServiceEndpoint endpoint = new ServiceEndpoint(
        new NetworkAddress("127.0.0.1", "ipv4"),
        new ServicePort("http", 4567)
    );

    Watson watson = new Watson(runtime, "ws://" + hubAddress, serviceName, endpoint);
    watson.registerHealthCheck(new BogusHealthCheck());
    watson.connect();
    watson.register();
  }

  public static class BogusHealthCheck implements HealthCheck {
    @Override public Boolean check() {
      try {
        final URL helloService = URI.create("http://127.0.0.1:4567").toURL();
        final HttpURLConnection conn = (HttpURLConnection) helloService.openConnection();
        return conn.getResponseCode() / 100 == 2;
      } catch (Exception any) {
        return false;
      }
    }
  }
}
