package dev.aulait.mousse.util;

import java.net.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the request method, URI, headers, and body at {@code DEBUG} level, similar to RestAssured's
 * {@code RequestLoggingFilter}.
 */
public class RequestLoggingFilter implements RestClientFilter {

  private static final Logger log = LoggerFactory.getLogger(RestClient.class);

  @Override
  public void logRequest(HttpRequest request, String requestBodyForLog) {
    if (!log.isDebugEnabled()) {
      return;
    }
    log.debug(
        "Request: "
            + request.method()
            + " "
            + request.uri()
            + RestClientLogSupport.NL
            + "Headers: "
            + RestClientLogSupport.formatHeaders(request.headers().map())
            + RestClientLogSupport.NL
            + "Body: "
            + (requestBodyForLog == null ? "<none>" : requestBodyForLog));
  }
}
