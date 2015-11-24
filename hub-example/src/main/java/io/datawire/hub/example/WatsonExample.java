package io.datawire.hub.example;


import hub.*;
import io.datawire.quark.runtime.Runtime;

import java.util.Random;

public class  WatsonExample {

  public static void main(String... args) {
    Runtime runtime = new io.datawire.quark.netty.QuarkNettyRuntime();

    String hubAddress = "localhost:8080";
    String serviceName = "foobar-service";
    ServiceEndpoint endpoint = new ServiceEndpoint(
        new NetworkAddress("127.0.0.1", "ipv4"),
        new ServicePort("http", 8080)
    );

    Watson watson = new Watson(runtime, "ws://" + hubAddress, serviceName, endpoint);
    watson.registerHealthCheck(new BogusHealthCheck());
    watson.connect();
    watson.register();
  }

  public static class BogusHealthCheck implements HealthCheck {
    @Override public Boolean check() {
      final Random random = new Random();
      return random.nextBoolean();
    }
  }
}
