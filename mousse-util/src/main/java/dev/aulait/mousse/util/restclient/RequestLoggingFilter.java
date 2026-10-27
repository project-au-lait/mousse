package dev.aulait.mousse.util.restclient;

import lombok.extern.slf4j.Slf4j;

/** Logs request details around a {@link RestClient} HTTP call. */
@Slf4j
public class RequestLoggingFilter implements RestClientFilter {

  private final HeaderLogMasker headerLogMasker = new HeaderLogMasker();
  private final RequestLoggingItems loggingItems = new RequestLoggingItems();

  @Override
  public <T> void filter(
      RequestWrapper request, ResponseWrapper<T> response, FilterContext context) {
    if (loggingItems.includes(RequestLoggingItems.Item.METHOD)) {
      log.info("Request method: {}", request.getRequest().method());
    }
    if (loggingItems.includes(RequestLoggingItems.Item.URI)) {
      log.info("Request URI: {}", request.getRequest().uri());
    }
    if (loggingItems.includes(RequestLoggingItems.Item.HEADER)) {
      log.info("Request headers: {}", headerLogMasker.mask(request.getRequest().headers().map()));
    }
    if (loggingItems.includes(RequestLoggingItems.Item.BODY) && log.isDebugEnabled()) {
      log.debug("Request body: {}", request.bodyAsString());
    }
    context.next(request, response);
  }
}
