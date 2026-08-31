package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the response status, headers, and body at {@code DEBUG} level, similar to RestAssured's
 * {@code ResponseLoggingFilter}.
 */
public class ResponseLoggingFilter implements RestClientFilter {

  private static final Logger log = LoggerFactory.getLogger(RestClient.class);

  @Override
  public void logResponse(HttpResponse<?> response) {
    if (!log.isDebugEnabled()) {
      return;
    }
    log.debug(
        "Response: "
            + response.statusCode()
            + RestClientLogSupport.NL
            + "Headers: "
            + RestClientLogSupport.formatHeaders(response.headers().map())
            + RestClientLogSupport.NL
            + "Body: {}",
        response.body());
  }
}
