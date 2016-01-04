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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static io.datawire.hub.config.ConfigurationParsingException.*;

public class ConfigurationFactory<T> {

  private final Class<T> clazz;
  private final ObjectMapper mapper;
  private final YAMLFactory yamlFactory;

  public ConfigurationFactory(Class<T> clazz, ObjectMapper mapper) {
    if (mapper == null) {
      throw new IllegalArgumentException("JSON/YAML mapper is null");
    }

    this.clazz = clazz;
    this.mapper = mapper.copy();
    this.yamlFactory = new YAMLFactory();
  }

  private void validate(ConfigurationSource source, T configuration) {
    // todo(plombardi): not important to implement this right now
  }

  private ConfigurationParsingException.Builder newParsingException(
      Throwable cause,
      ConfigurationParsingException.SourceLocation location,
      String summary) {

    return ConfigurationParsingException.builder(summary).withCause(cause).withSourceLocation(location);
  }

  private T build(JsonNode node, ConfigurationSource source) throws ConfigurationException, IOException {
    try {
      final T config = mapper.readValue(new TreeTraversingParser(node), clazz);
      validate(source, config);
      return config;
    } catch (UnrecognizedPropertyException ex) {
      throw newParsingException(ex, new SourceLocation(ex.getLocation()), "Unrecognized field/property").build(source);
    } catch (InvalidFormatException ex) {
      String sourceType = ex.getValue().getClass().getSimpleName();
      String targetType = ex.getTargetType().getSimpleName();
      throw newParsingException(ex, new SourceLocation(ex.getLocation()), "Incorrect data type of value")
          .withDetail("is of type: " + sourceType + ", expected: " + targetType)
          .withFieldPathInfo(ex.getPath())
          .build(source);
    } catch (JsonMappingException ex) {
      throw newParsingException(ex, new SourceLocation(ex.getLocation()), "Failed parsing configuration source")
          .withDetail(ex.getMessage())
          .build(source);
    }
  }

  public T build(ConfigurationSource source) throws ConfigurationException, IOException {
    try (InputStream input = source.open()) {
      final JsonNode root = mapper.readTree(yamlFactory.createParser(input));

      if (root == null) {
        throw ConfigurationParsingException.builder("Configuration is empty").build(source);
      }

      return build(root, source);
    } catch (YAMLException ex) {
      ConfigurationParsingException.Builder builder = ConfigurationParsingException.builder("Malformed YAML")
          .withCause(ex)
          .withDetail(ex.getMessage());

      if (ex instanceof MarkedYAMLException) {
        builder.withSourceLocation(new SourceLocation(((MarkedYAMLException) ex).getProblemMark()));
      }

      throw builder.build(source);
    }
  }

  public T build(File file) throws ConfigurationException, IOException {
    return build(new FileConfigurationSource(file));
  }

  public T build(Path file) throws ConfigurationException, IOException {
    return build(new FileConfigurationSource(file));
  }
}
