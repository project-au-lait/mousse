package dev.aulait.mousse.util;

import java.io.PrintStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/** Logs request and response details around a {@link RestClient} HTTP call. */
@Slf4j
public class RestClientLoggingFilter implements RestClientFilter {

  private final PrintStream stream;

  /** Creates a filter that logs at debug level through SLF4J. */
  public RestClientLoggingFilter() {
    this.stream = null;
  }

  /** Creates a filter that logs to the supplied stream. */
  public RestClientLoggingFilter(PrintStream stream) {
    this.stream = Objects.requireNonNull(stream);
  }

  @Override
  public <T> HttpResponse<T> filter(
      RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler, FilterContext context) {
    write("Request method: " + request.method());
    write("Request URI: " + request.uri());
    write("Request body: " + request.bodyAsString());

    HttpResponse<T> response = context.next(request, bodyHandler);

    write("Response status: " + response.statusCode());
    write("Response body: " + bodyAsString(response.body()));
    return response;
  }

  private void write(String message) {
    if (stream == null) {
      log.debug(message);
    } else {
      stream.println(message);
    }
  }

  private String bodyAsString(Object body) {
    if (body instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    return Objects.toString(body, "");
  }
}
