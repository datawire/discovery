package io.datawire.util;


import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Scanner;

public class Fixtures {

  public static class FixtureNotFound extends RuntimeException {

    private final String path;

    FixtureNotFound(String path) {
      super("Cannot find fixture (path: " + path + ")");
      this.path = path;
    }

    public String getPath() {
      return path;
    }
  }

  public InputStream loadFixture(String path) {
    if (isBlank(path)) {
      throw new IllegalArgumentException("Fixture path is empty, all whitespace or null");
    }

    String truePath = buildFixturePath(path);
    InputStream in = Fixtures.class.getResourceAsStream(truePath);

    if (in != null) {
      return in;
    } else {
      throw new FixtureNotFound(truePath);
    }
  }

  public String loadFixtureAsString(String path, Charset charset) {
    if (charset == null) {
      throw new IllegalArgumentException("Fixture charset is null");
    }

    Scanner s = new Scanner(loadFixture(path), charset.toString()).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  private static String buildFixturePath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("Fixture path is null");
    }

    String result = path;
    if (!result.startsWith("/")) {
      result = "/" + result;
    }

    if (!result.startsWith("/fixtures")) {
      result = "/fixtures" + result;
    }

    return result;
  }

  private static boolean isBlank(String string) {
    return string == null || string.trim().isEmpty();
  }
}