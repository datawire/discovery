package io.datawire.hub.config;


import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetedVariableLookup extends StrSubstitutor {

  private final static Pattern VARIABLE_PATTERN = Pattern.compile("^(\\w+)\\s+`(.*)`$");

  private final static String FILE_TARGET="file";
  private final static String ENVIRONMENT_VARIABLE_TARGET="env";

  private final boolean strict;
  private final Map<String, String> capturedEnvironmentVariables;

  public TargetedVariableLookup() {
    this(Collections.unmodifiableMap(System.getenv()), true);
  }

  public TargetedVariableLookup(Map<String, String> capturedEnvironmentVariables,  boolean strict) {
    this.capturedEnvironmentVariables = capturedEnvironmentVariables;
    this.strict = strict;
  }

  public String lookup(String key) {
    Matcher matcher = VARIABLE_PATTERN.matcher(key);
    if (matcher.matches()) {
      String type = matcher.group(1);
      String source = matcher.group(2);

      String value = null;
      switch (type.toLowerCase()) {
        case FILE_TARGET:
          value = readContentFromFile(key);
          break;
        case ENVIRONMENT_VARIABLE_TARGET:
          value = capturedEnvironmentVariables.get(source);
          break;
      }

      if (value == null && strict) {
        throw new UndefinedVariableException(type, source);
      }

      return value;
    } else {
      throw new InvalidVariableFormatException(key, VARIABLE_PATTERN);
    }
  }

  private String readContentFromFile(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      return null;
    }
  }
}
