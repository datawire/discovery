package io.datawire.util

import org.assertj.core.api.Assertions.*
import org.junit.Test


class FixturesTest {

  private val fixtures = Fixtures()

  @Test fun loadFixtureAsString() {
    val data = fixtures.loadFixtureAsString("foobar", Charsets.UTF_8)
    assertThat(data).isEqualTo("foobar")
  }

  @Test fun loadFixture() {
    val stream = fixtures.loadFixture("foobar")
    assertThat(stream).isNotNull()
  }

  @Test fun failLoadingNonexistentFixture() {
    try {
      fixtures.loadFixture("does-not-exist")
      failBecauseExceptionWasNotThrown(Fixtures.FixtureNotFound::class.java)
    } catch (any: Fixtures.FixtureNotFound) {
      assertThat(any).hasMessage("Cannot find fixture (path: /fixtures/does-not-exist)")
    }
  }

  @Test fun failLoadingFixtureAsStringWithNullCharset() {
    try {
      fixtures.loadFixtureAsString("irrelevant", null)
      failBecauseExceptionWasNotThrown(IllegalArgumentException::class.java)
    } catch (ex: IllegalArgumentException) {
      assertThat(ex).hasMessage("Fixture charset is null")
    }
  }

  @Test fun failLoadingFixtureAsStringWithNullEmptyOrAllWhitespacePath() {
    for (it in listOf("", null, "   ")) {
      try {
        fixtures.loadFixtureAsString("", Charsets.UTF_8)
        failBecauseExceptionWasNotThrown(IllegalArgumentException::class.java)
      } catch (ex: IllegalArgumentException) {
        assertThat(ex).hasMessage("Fixture path is empty, all whitespace or null")
      }
    }
  }
}