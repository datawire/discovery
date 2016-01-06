package io.datawire.hub.config;


public class UndefinedVariableException extends RuntimeException {

  private final String identifier;
  private final String source;

  public UndefinedVariableException(String type, String source) {
    super(String.format(
        "Variable not defined. Unable to substitute expression. (type: %s, source: %s, expression: %s)",
        type, source, "${" + type + " `" + source+ "`}"));

    this.source = type;
    this.identifier = source;
  }

  public String getVariableIdentifier() {
    return identifier;
  }

  public String getVariableSource() {
    return source;
  }
}
