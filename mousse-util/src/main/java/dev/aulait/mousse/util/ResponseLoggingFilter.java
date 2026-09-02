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

  private final boolean prettyPrint;

  /** Creates a filter that pretty-prints JSON bodies. */
  public ResponseLoggingFilter() {
    this(true);
  }

  /**
   * Creates a filter with explicit control over JSON body formatting.
   *
   * @param prettyPrint {@code true} to pretty-print JSON bodies, {@code false} to log them compact
   */
  public ResponseLoggingFilter(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  @Override
  public void logResponse(HttpResponse<?> response) {
    if (!log.isDebugEnabled()) {
      return;
    }
    String prefix =
        "Response: "
            + response.statusCode()
            + RestClientLogSupport.NL
            + "Headers: "
            + RestClientLogSupport.formatHeaders(response.headers().map())
            + RestClientLogSupport.NL
            + "Body:";
    Object body = response.body();
    if (body instanceof String stringBody) {
      // pretty-print JSON bodies; keep SLF4J's {} placeholder for non-String bodies (e.g. byte[])
      log.debug(prefix + RestClientLogSupport.formatResponseBody(stringBody, prettyPrint));
    } else {
      log.debug(prefix + " {}", body);
    }
  }
}
