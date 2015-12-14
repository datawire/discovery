package io.datawire.hub.example;

import static spark.Spark.*;

public class HelloWorldService {

  private final static int DEFAULT_PORT = 4567;

  public static void main(String... args) throws Exception {
    port(args.length > 0 ? Integer.valueOf(args[0]) : DEFAULT_PORT);

    get("/", (req, resp) -> "Hello, world!");
    get("/health", (req, resp) -> "OK");
  }
}
