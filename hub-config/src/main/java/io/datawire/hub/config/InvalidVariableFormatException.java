package io.datawire.hub.config;


import java.util.regex.Pattern;

public class InvalidVariableFormatException extends RuntimeException {

  public InvalidVariableFormatException(String format, Pattern pattern) {
    super("Invalid variable declaration format (was: " + format + ", expected: " + pattern.toString() +")");
  }
}
