package io.datawire.hub.config;


import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class SubstitutingSource implements ConfigurationSource {

  private final ConfigurationSource delegate;
  private final StrSubstitutor substitutor;

  public SubstitutingSource(ConfigurationSource delegate, StrSubstitutor substitutor) {
    this.delegate = delegate;
    this.substitutor = substitutor;
  }

  @Override
  public String getLocation() {
    return delegate.getLocation();
  }

  public InputStream open() throws IOException {
    final String config = read(delegate.open());
    final String substituted = substitutor.replace(config);
    return new ByteArrayInputStream(substituted.getBytes(StandardCharsets.UTF_8));
  }

  private static String read(InputStream input) throws IOException {
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
      return buffer.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }
}
