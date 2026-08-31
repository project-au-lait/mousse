package dev.aulait.mousse.util;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Intercepts requests and responses sent by {@link RestClient}, similar to RestAssured's {@code
 * Filter}.
 *
 * <p>Attach filters via {@code RestClient.builder().filters(new RequestLoggingFilter(), new
 * ResponseLoggingFilter())}. Without any filters attached, {@link RestClient} sends requests
 * without any logging, just like RestAssured without {@code .filters(...)}.
 */
public interface RestClientFilter {

  /**
   * Called with a request before it is sent.
   *
   * @param request the request to be sent
   * @param requestBodyForLog the request body to log, or {@code null} if there is no body
   */
  default void logRequest(HttpRequest request, String requestBodyForLog) {}

  /**
   * Called with a response after it is received.
   *
   * @param response the received response
   */
  default void logResponse(HttpResponse<?> response) {}
}
