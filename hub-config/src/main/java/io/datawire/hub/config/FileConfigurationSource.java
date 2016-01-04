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

import java.io.*;
import java.nio.file.Path;

public class FileConfigurationSource implements ConfigurationSource {

  private final File file;

  public FileConfigurationSource(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File is null");
    }

    this.file = file;
  }

  public FileConfigurationSource(Path path) {
    this(path.toFile());
  }

  public String getLocation() {
    return file.getPath();
  }

  @Override
  public InputStream open() throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException(String.format("File does not exist (path: %s)", file.getPath()));
    }

    return new FileInputStream(file);
  }
}
