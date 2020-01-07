package com.kgregorczyk.bank.filters;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.concurrent.TimeUnit;

/**
 * Logging filter which is applied to every incoming request.
 *
 * <p>Example: `127.0.0.1 GET [/api/users]`
 */
@Slf4j
public class LoggingFilter implements Filter {

  @Override
  public void handle(final Request request, final Response response) {
    final Stopwatch watch = request.attribute("watch");
    log.info(
        "{} {} [{}] in {} ms",
        request.ip(),
        request.requestMethod(),
        request.pathInfo(),
        watch.stop().elapsed(TimeUnit.MILLISECONDS));
  }
}
