package dev.aulait.mousse.util;

import java.util.List;
import java.util.Map;

/**
 * Base class for {@link RequestLoggingFilter} and {@link ResponseLoggingFilter}, holding the
 * shared JSON body formatting setting and header formatting logic.
 */
abstract class AbstractLoggingFilter implements RestClientFilter {

  static final String NL = System.lineSeparator();

  final boolean prettyPrint;

  AbstractLoggingFilter() {
    this(true);
  }

  AbstractLoggingFilter(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

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
}
