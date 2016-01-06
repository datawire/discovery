package io.datawire.hub.config;


import java.util.regex.Pattern;

public class InvalidVariableFormatException extends RuntimeException {

  public InvalidVariableFormatException(String format, Pattern pattern) {
    super("Invalid variable format (was: " + format + ")");
  }
}
