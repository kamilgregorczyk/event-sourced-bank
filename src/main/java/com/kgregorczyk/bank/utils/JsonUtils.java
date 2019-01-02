package com.kgregorczyk.bank.utils;

import com.google.gson.Gson;


public class JsonUtils {

  private static final Gson GSON = new Gson();

  private JsonUtils() {
  }

  /**
   * Translates any {@code} model to JSON formatted string.
   */
  public static String toJson(Object model) {
    return GSON.toJson(model);
  }
}