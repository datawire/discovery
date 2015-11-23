package io.datawire.hub.example;


import hub.NetworkAddress;
import hub.ServiceEndpoint;
import hub.ServicePort;
import hub.Watson;
import io.datawire.quark.runtime.Runtime;

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
  }
}
