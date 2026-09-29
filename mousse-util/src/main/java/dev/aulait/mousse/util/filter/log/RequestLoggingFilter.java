package dev.aulait.mousse.util.filter.log;

import dev.aulait.mousse.util.RequestWrapper;
import dev.aulait.mousse.util.ResponseWrapper;
import dev.aulait.mousse.util.RestClient;
import dev.aulait.mousse.util.filter.FilterContext;
import dev.aulait.mousse.util.filter.RestClientFilter;
import lombok.extern.slf4j.Slf4j;

/** Logs request details around a {@link RestClient} HTTP call. */
@Slf4j
public class RequestLoggingFilter implements RestClientFilter {

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> response, FilterContext context) {
    log.info("Request method: {}", request.getRequest().method());
    log.info("Request URI: {}", request.getRequest().uri());
    log.info("Request headers: {}", request.getRequest().headers().map());
    if (log.isDebugEnabled()) {
      log.debug("Request body: {}", request.bodyAsString());
    }
    context.next(request, response);
  }
}
