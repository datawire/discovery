package io.datawire.hub.config;


public class UndefinedVariableException extends RuntimeException {

  private final String identifier;
  private final String source;

  public UndefinedVariableException(String source, String identifier) {
    super(String.format(
        "Variable not defined. Unable to substitute expression. (variable: %s, source: %s, expression: %s)",
        identifier, source, "${" + identifier+ "}"));

    this.source = source;
    this.identifier = identifier;
  }

  public String getVariableIdentifier() {
    return identifier;
  }

  public String getVariableSource() {
    return source;
  }
}
