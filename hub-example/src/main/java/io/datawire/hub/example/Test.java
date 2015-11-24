package io.datawire.hub.example;


import hub.*;
import io.datawire.quark.runtime.Runtime;

import java.util.Random;

public class Test {

  public static void main(String... args) {
    Runtime runtime = new io.datawire.quark.netty.QuarkNettyRuntime();

    //new hub.TestTask(runtime);
    new ChildA(runtime);
  }
}
