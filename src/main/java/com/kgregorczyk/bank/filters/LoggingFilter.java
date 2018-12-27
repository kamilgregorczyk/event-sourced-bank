package com.kgregorczyk.bank.filters;

import lombok.extern.slf4j.Slf4j;
import spark.Filter;
import spark.Request;
import spark.Response;

/**
 * Logging filter which is applied to every incoming request.
 *
 * <p>Example: `127.0.0.1 GET [/api/users]`</p>
 */
@Slf4j
public class LoggingFilter implements Filter {

  @Override
  public void handle(final Request request, final Response response) {
    log.info("{} {} [{}]", request.ip(), request.requestMethod(), request.pathInfo());
  }
}
