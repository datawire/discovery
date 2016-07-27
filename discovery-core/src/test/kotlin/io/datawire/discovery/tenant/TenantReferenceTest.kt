package io.datawire.discovery.tenant

import org.junit.Test
import org.assertj.core.api.Assertions.*


class TenantReferenceTest {

  @Test fun noneUserIsStaticallyDefined() {
    assertThat(TenantReference.NONE_USER).isEqualTo("none")
  }

  @Test fun toJsonConstructsValidJsonObjectWhenUserIsNotSet() {
    val ref = TenantReference("ABC123XYZ")
    val refJson = ref.toJson()

    assertThat(refJson.getString("id")).isEqualTo("ABC123XYZ")
    assertThat(refJson.getString("user")).isEqualTo(TenantReference.NONE_USER)
  }

  @Test fun toJsonConstructsValidJsonObjectWhenUserIsSet() {
    val ref = TenantReference("ABC123XYZ", "dev@datawire.io")
    val refJson = ref.toJson()

    assertThat(refJson.getString("id")).isEqualTo("ABC123XYZ")
    assertThat(refJson.getString("user")).isEqualTo("dev@datawire.io")
  }
}