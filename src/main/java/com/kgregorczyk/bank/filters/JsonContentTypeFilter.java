package com.kgregorczyk.bank.filters;

import spark.Filter;
import spark.Request;
import spark.Response;

/**
 * Sets ContentType:application/json header.
 */
public class JsonContentTypeFilter implements Filter {

  @Override
  public void handle(Request request, Response response) {
    response.header("Content-Type", "application/json");
  }
}
