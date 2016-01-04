/*
 * Copyright 2015 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.hub.config;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ConfigurationParsingException extends ConfigurationException {

  ConfigurationParsingException(ConfigurationSource source, String message) {
    this(source, message, null);
  }

  ConfigurationParsingException(ConfigurationSource source, String message, Throwable cause) {
    super(source, Collections.singletonList(message), cause);
  }

  public static ConfigurationParsingException.Builder builder(String summary) {
    return new Builder(summary);
  }

  public final static class SourceLocation {

    private final int lineNumber;
    private final int columnNumber;

    SourceLocation(int lineNumber, int columnNumber) {
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    SourceLocation(Mark mark) {
      this.lineNumber = mark.getLine();
      this.columnNumber = mark.getColumn();
    }

    SourceLocation(JsonLocation jsonLocation) {
      this.lineNumber = jsonLocation.getLineNr();
      this.columnNumber = jsonLocation.getColumnNr();
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public int getColumnNumber() {
      return columnNumber;
    }

    @Override public boolean equals(Object object) {
      if (this == object) {
        return true;
      }

      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      SourceLocation other = (SourceLocation) object;
      return Objects.equals(lineNumber, other.lineNumber) &&
          Objects.equals(columnNumber, other.columnNumber);
    }

    @Override public int hashCode() {
      return Objects.hash(lineNumber, columnNumber);
    }
  }

  public static class Builder {

    private String summary;
    private String detail;
    private Throwable cause;
    private SourceLocation location;

    private List<JsonMappingException.Reference> fieldPathInfo = Collections.emptyList();

    Builder(String summary) {
      this.summary = summary;
    }

    Builder withCause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    Builder withDetail(String detail) {
      this.detail = detail;
      return this;
    }

    Builder withSourceLocation(SourceLocation location) {
      this.location = location;
      return this;
    }

    Builder withFieldPathInfo(List<JsonMappingException.Reference> fieldPathInfo) {
      this.fieldPathInfo = fieldPathInfo;
      return this;
    }

    ConfigurationParsingException build(ConfigurationSource source) {
      StringBuilder result = new StringBuilder(summary);

      if (fieldPathInfo != null && !fieldPathInfo.isEmpty()) {
        result.append(" at: ").append(buildPath());
      } else if (location != null) {
        result.append(" at line: ").append(location.getLineNumber() + 1)
            .append(", column: ").append(location.getColumnNumber() + 1);
      }

      if (detail != null) {
        result.append("; ").append(detail);
      }

      return new ConfigurationParsingException(source, result.toString(), cause);
    }

    private String buildPath() {
      StringBuilder result = new StringBuilder();

      if (fieldPathInfo != null) {
        Iterator<JsonMappingException.Reference> it = fieldPathInfo.iterator();
        while (it.hasNext()) {
          final JsonMappingException.Reference reference = it.next();
          final String name = reference.getFieldName();

          if (name == null) {
            result.append('[').append(reference.getIndex()).append(']');
          } else {
            result.append(name);
          }

          if (it.hasNext()) {
            result.append('.');
          }
        }
      }

      return result.toString();
    }
  }
}
