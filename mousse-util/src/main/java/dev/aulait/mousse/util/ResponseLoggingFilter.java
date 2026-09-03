package dev.aulait.mousse.util;

import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the response status, headers, and body at {@code DEBUG} level, similar to RestAssured's
 * {@code ResponseLoggingFilter}.
 */
public class ResponseLoggingFilter extends AbstractLoggingFilter {

  private static final Logger log = LoggerFactory.getLogger(RestClient.class);

  /** Creates a filter that pretty-prints JSON bodies. */
  public ResponseLoggingFilter() {
    super();
  }

  /**
   * Creates a filter with explicit control over JSON body formatting.
   *
   * @param prettyPrint {@code true} to pretty-print JSON bodies, {@code false} to log them compact
   */
  public ResponseLoggingFilter(boolean prettyPrint) {
    super(prettyPrint);
  }

  @Override
  public void logResponse(HttpResponse<?> response) {
    if (!log.isDebugEnabled()) {
      return;
    }
    String prefix =
        "Response: "
            + response.statusCode()
            + NL
            + "Headers: "
            + formatHeaders(response.headers().map())
            + NL
            + "Body:";
    Object body = response.body();
    if (body instanceof String stringBody) {
      // pretty-print JSON bodies; keep SLF4J's {} placeholder for non-String bodies (e.g. byte[])
      log.debug(prefix + formatBody(stringBody));
    } else {
      log.debug(prefix + " {}", body);
    }
  }

  /**
   * Formats a response body string for display: pretty-printed on its own line when {@link
   * #prettyPrint} is true and the body is valid JSON; otherwise returned as-is on the same line.
   */
  private String formatBody(String body) {
    if (!prettyPrint) {
      return " " + body;
    }
    try {
      return NL + JsonUtils.obj2fmtstr(JsonUtils.str2obj(body, Object.class));
    } catch (UncheckedIOException e) {
      // not valid JSON (e.g. plain text or an HTML error page); fall back to the raw body
      return " " + body;
    }
  }
}
