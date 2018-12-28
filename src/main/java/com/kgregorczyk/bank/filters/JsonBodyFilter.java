package com.kgregorczyk.bank.filters;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static spark.Spark.halt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.APIResponse.Status;
import java.io.IOException;
import spark.Filter;
import spark.Request;
import spark.Response;

/**
 * Verifies if incoming body is not empty and  in JSON format.
 */
public class JsonBodyFilter implements Filter {

  private static final ImmutableSet<String> HTTP_METHODS_WITH_BODY = ImmutableSet
      .of("POST", "PUT", "PATCH");

  private static boolean isJSONValid(String jsonInString) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      mapper.readTree(jsonInString);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public void handle(Request request, Response response) {
    if (HTTP_METHODS_WITH_BODY.contains(request.requestMethod())) {
      if (request.body().isEmpty()) {
        halt(HTTP_BAD_REQUEST,
            new APIResponse(Status.ERROR, "Body is empty").toJson());

      } else if (!isJSONValid(request.body())) {
        halt(HTTP_BAD_REQUEST,
            new APIResponse(Status.ERROR, "Body is required and it is not in JSON format")
                .toJson());
      }
    }

  }
}
