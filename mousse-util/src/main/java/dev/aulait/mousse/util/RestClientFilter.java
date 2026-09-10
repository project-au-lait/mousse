package dev.aulait.mousse.util;

import java.net.http.HttpResponse;

/** Intercepts a REST request and its response around the actual HTTP call. */
@FunctionalInterface
public interface RestClientFilter {

  /**
   * Filters a request and response.
   *
   * @param request the request to inspect or replace
   * @param bodyHandler the response body handler
   * @param context the remaining filter chain
   * @param <T> the response body type
   * @return the response returned by the remaining chain
   */
  <T> HttpResponse<T> filter(
      RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler, FilterContext context);

  /** Continues processing with the next filter, or sends the request at the end of the chain. */
  interface FilterContext {

    /**
     * Continues processing the request.
     *
     * @param request the request to pass to the next filter
     * @param bodyHandler the response body handler
     * @param <T> the response body type
     * @return the response returned by the remaining chain
     */
    <T> HttpResponse<T> next(RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler);
  }
}
