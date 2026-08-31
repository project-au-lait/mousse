package dev.aulait.mousse.util;

import java.util.List;
import java.util.Map;

/** Shared log formatting used by {@link RequestLoggingFilter} and {@link ResponseLoggingFilter}. */
final class RestClientLogSupport {

  static final String NL = System.lineSeparator();

  private RestClientLogSupport() {}

  static String formatHeaders(Map<String, List<String>> headers) {
    if (headers.isEmpty()) {
      return "<none>";
    }
    StringBuilder sb = new StringBuilder();
    headers.forEach(
        (name, values) ->
            sb.append(NL).append('\t').append(name).append(": ").append(String.join(", ", values)));
    return sb.toString();
  }

  /**
   * Formats a request/response body for display: JSON bodies are pretty-printed on their own
   * lines (matching amv/RestAssured's log style); non-JSON bodies are kept as-is on the same line.
   */
  static String formatBody(String body) {
    if (body == null) {
      return " <none>";
    }
    String prettyJson = JsonUtils.tryPrettyPrint(body);
    return prettyJson != null ? NL + prettyJson : " " + body;
  }
}
