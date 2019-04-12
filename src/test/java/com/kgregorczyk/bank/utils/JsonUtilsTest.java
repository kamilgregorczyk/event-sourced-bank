package com.kgregorczyk.bank.utils;

import com.google.common.truth.Truth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

class JsonUtilsTest {

  @Test
  public void toJson() {
    Truth.assertThat(JsonUtils.toJson(new DummyModel("Avengers")))
        .isEqualTo("{\"niceValue\":\"Avengers\"}");
  }

  @Test
  void isJSONValidValidJson() {
    assertThat(JsonUtils.isJSONValid("{\"niceValue\":\"Avengers\"}")).isTrue();
  }

  @Test
  void isJSONValidInvalidJson() {
    assertThat(JsonUtils.isJSONValid("{\"niceValue\":}")).isFalse();
    assertThat(JsonUtils.isJSONValid("{niceValue;Avengers}")).isFalse();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyModel {

    String niceValue;
  }
}
