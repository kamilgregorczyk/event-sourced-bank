package com.kgregorczyk.bank.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class JsonUtils {

  private static final Gson GSON = new Gson();

  private JsonUtils() {}

  /** Translates any {@code} model to JSON formatted string. */
  public static String toJson(Object model) {
    return GSON.toJson(model);
  }

  /** Checks whether {@code jsonInString} is a valid JSON formatted string. */
  public static boolean isJSONValid(String jsonInString) {
    try {
      GSON.fromJson(jsonInString, Object.class);
      return true;
    } catch (JsonSyntaxException e) {
      return false;
    }
  }
}
