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

import java.util.Collections;
import java.util.List;

public class ConfigurationException extends Exception {

  private final static String NEWLINE = System.lineSeparator();

  private final List<String> errors;

  ConfigurationException(ConfigurationSource source, List<String> errors) {
    super(formatMessage(source, errors));
    this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
  }

  ConfigurationException(ConfigurationSource source, List<String> errors, Throwable cause) {
    super(formatMessage(source, errors), cause);
    this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
  }

  public List<String> getErrors() {
    return errors;
  }

  private static String formatMessage(ConfigurationSource source, List<String> errors) {
    final StringBuilder result = new StringBuilder(source.getLocation());
    result.append(" has one or more errors:").append(NEWLINE);

    for (String error : errors) {
      result.append("  * ").append(error).append(NEWLINE);
    }

    return result.toString();
  }
}
