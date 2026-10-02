package dev.aulait.mousse.util.restclient;

import java.net.http.HttpResponse;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Logs response details around a {@link RestClient} HTTP call. */
@Slf4j
public class ResponseLoggingFilter implements RestClientFilter {

  private final HeaderLogConfig headerLogConfig;

  public ResponseLoggingFilter() {
    this(HeaderLogConfig.builder().build());
  }

  public ResponseLoggingFilter(HeaderLogConfig headerLogConfig) {
    this.headerLogConfig = Objects.requireNonNull(headerLogConfig);
  }

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> responseWrapper, FilterContext context) {
    context.next(request, responseWrapper);
    HttpResponse<T> response = responseWrapper.getResponse();
    log.info("Response status: {}", response.statusCode());
    if (log.isInfoEnabled()) {
      log.info("Response headers: {}", headerLogConfig.headersForLogging(response.headers().map()));
    }
    if (log.isDebugEnabled()) {
      log.debug("Response body: {}", responseWrapper.bodyAsString());
    }
  }
}
