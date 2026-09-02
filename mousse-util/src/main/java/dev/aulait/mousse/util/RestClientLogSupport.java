package dev.aulait.mousse.util;

import java.io.UncheckedIOException;
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
   * Formats a request body for display using the original (pre-serialization) object: {@code
   * null} becomes {@code "<none>"}; a {@code String} (e.g. the multipart part summary) is used
   * as-is; any other object is serialized via {@link JsonUtils}, pretty-printed on its own line
   * when {@code prettyPrint} is true, or compact on the same line otherwise.
   */
  static String formatRequestBody(Object body, boolean prettyPrint) {
    if (body == null) {
      return " <none>";
    }
    if (body instanceof String text) {
      return " " + text;
    }
    return prettyPrint ? NL + JsonUtils.obj2fmtstr(body) : " " + JsonUtils.obj2str(body);
  }

  /**
   * Formats a response body string for display: pretty-printed on its own line when {@code
   * prettyPrint} is true and the body is valid JSON; otherwise returned as-is on the same line.
   */
  static String formatResponseBody(String body, boolean prettyPrint) {
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
