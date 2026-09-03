package dev.aulait.mousse.util;

import java.net.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs the request method, URI, headers, and body at {@code DEBUG} level, similar to RestAssured's
 * {@code RequestLoggingFilter}.
 */
public class RequestLoggingFilter extends AbstractLoggingFilter {

  private static final Logger log = LoggerFactory.getLogger(RestClient.class);

  /** Creates a filter that pretty-prints JSON bodies. */
  public RequestLoggingFilter() {
    super();
  }

  /**
   * Creates a filter with explicit control over JSON body formatting.
   *
   * @param prettyPrint {@code true} to pretty-print JSON bodies, {@code false} to log them compact
   */
  public RequestLoggingFilter(boolean prettyPrint) {
    super(prettyPrint);
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
            + NL
            + "Headers: "
            + formatHeaders(request.headers().map())
            + NL
            + "Body:"
            + formatBody(requestBody));
  }

  /**
   * Formats a request body for display using the original (pre-serialization) object: {@code
   * null} becomes {@code "<none>"}; a {@code String} (e.g. the multipart part summary) is used
   * as-is; any other object is serialized via {@link JsonUtils}, pretty-printed on its own line
   * when {@link #prettyPrint} is true, or compact on the same line otherwise.
   */
  private String formatBody(Object body) {
    if (body == null) {
      return " <none>";
    }
    if (body instanceof String text) {
      return " " + text;
    }
    return prettyPrint ? NL + JsonUtils.obj2fmtstr(body) : " " + JsonUtils.obj2str(body);
  }
}
