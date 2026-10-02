package dev.aulait.mousse.util.restclient;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Logs request details around a {@link RestClient} HTTP call. */
@Slf4j
public class RequestLoggingFilter implements RestClientFilter {

  private final HeaderLogConfig headerLogConfig;

  public RequestLoggingFilter() {
    this(HeaderLogConfig.builder().build());
  }

  public RequestLoggingFilter(HeaderLogConfig headerLogConfig) {
    this.headerLogConfig = Objects.requireNonNull(headerLogConfig);
  }

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> response, FilterContext context) {
    log.info("Request method: {}", request.getRequest().method());
    log.info("Request URI: {}", request.getRequest().uri());
    if (log.isInfoEnabled()) {
      log.info(
          "Request headers: {}",
          headerLogConfig.headersForLogging(request.getRequest().headers().map()));
    }
    if (log.isDebugEnabled()) {
      log.debug("Request body: {}", request.bodyAsString());
    }
    context.next(request, response);
  }
}
