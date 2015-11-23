package io.datawire.hub.example;


import hub.NetworkAddress;
import hub.ServiceEndpoint;
import hub.ServicePort;
import hub.Watson;
import io.datawire.quark.runtime.Runtime;

public class WatsonExample {

  public static void main(String... args) {
    Runtime runtime = new io.datawire.quark.netty.QuarkNettyRuntime();

    String hubAddress = "";
    String serviceName = "foobar-service";
    ServiceEndpoint endpoint = new ServiceEndpoint(
        new NetworkAddress("127.0.0.1", "tcp"),
        new ServicePort("http", 8080)
    );

    Watson watson = new Watson(runtime, "http://localhost:8081", "foobar", endpoint);
    watson.register();
  }
}
