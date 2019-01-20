package com.kgregorczyk.bank.filters;

import static com.kgregorczyk.bank.utils.JsonUtils.isJSONValid;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static spark.Spark.halt;

import com.google.common.collect.ImmutableSet;
import com.kgregorczyk.bank.controllers.dto.APIResponse;
import com.kgregorczyk.bank.controllers.dto.APIResponse.Status;
import spark.Filter;
import spark.Request;
import spark.Response;

/** Verifies if incoming body is not empty and in JSON format. */
public class JsonBodyFilter implements Filter {

  private static final ImmutableSet<String> HTTP_METHODS_WITH_BODY = ImmutableSet.of("POST", "PUT");

  private static void stopRequest(String message) {
    halt(HTTP_BAD_REQUEST, new APIResponse(Status.ERROR, message).toJson());
  }

  @Override
  public void handle(Request request, Response response) {

    if (HTTP_METHODS_WITH_BODY.contains(request.requestMethod())) {
      if (request.body().isEmpty()) {
        stopRequest("Body is empty");

      } else if (!isJSONValid(request.body())) {
        stopRequest("Body is required and it is not in JSON format");
      }
    }
  }
}
