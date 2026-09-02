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

  private final boolean prettyPrint;

  /** Creates a filter that pretty-prints JSON bodies. */
  public RequestLoggingFilter() {
    this(true);
  }

  /**
   * Creates a filter with explicit control over JSON body formatting.
   *
   * @param prettyPrint {@code true} to pretty-print JSON bodies, {@code false} to log them compact
   */
  public RequestLoggingFilter(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  @Override
  public void logRequest(HttpRequest request, Object requestBody) {
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
            + "Body:"
            + RestClientLogSupport.formatRequestBody(requestBody, prettyPrint));
  }
}
