package dev.aulait.mousse.util;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** An HTTP request and its repeatable body, exposed to {@link RestClientFilter filters}. */
public record RestClientRequest(HttpRequest request, byte[] body) {
  public RestClientRequest(HttpRequest request, byte[] body) {
    this.request = Objects.requireNonNull(request);
    this.body = Objects.requireNonNull(body).clone();
  }

  public String method() {
    return request.method();
  }

  public URI uri() {
    return request.uri();
  }

  public HttpHeaders headers() {
    return request.headers();
  }

  public byte[] body() {
    return body.clone();
  }

  public String bodyAsString() {
    return new String(body, StandardCharsets.UTF_8);
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof RestClientRequest(HttpRequest thatRequest, byte[] thatBody)
            && request.equals(thatRequest)
            && Arrays.equals(body, thatBody);
  }

  @Override
  public int hashCode() {
    return 31 * request.hashCode() + Arrays.hashCode(body);
  }

  @Override
  public String toString() {
    return "RestClientRequest[request=" + request + ", body=" + Arrays.toString(body) + "]";
  }
}
