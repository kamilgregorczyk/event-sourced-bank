package com.kgregorczyk.bank.filters;

import com.google.common.base.Stopwatch;
import spark.Filter;
import spark.Request;
import spark.Response;

public class StartDateApplyingFilter implements Filter {

  @Override
  public void handle(final Request request, final Response response) {
    request.attribute("watch", Stopwatch.createStarted());
  }
}
