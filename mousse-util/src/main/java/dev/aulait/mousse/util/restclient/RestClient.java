package dev.aulait.mousse.util.restclient;

import dev.aulait.mousse.util.JsonType;
import dev.aulait.mousse.util.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * HTTP REST client using {@link java.net.http.HttpClient} and Jackson for JSON serialization.
 *
 * <p>Provides methods for GET, POST, PUT, and DELETE operations with automatic JSON
 * serialization/deserialization.
 */
@Builder
@Slf4j
public class RestClient {

  private static final String CONTENT_DISPOSITION_PREFIX =
      "Content-Disposition: form-data; name=\"";

  @Getter private String baseUrl;
  @Getter @Builder.Default private long connectTimeoutMillis = 5000L;
  @Getter @Singular private Map<String, String> headers;
  @Getter @Singular private Map<String, Supplier<String>> headerSuppliers;
  @Getter @Singular private List<RestClientFilter> filters;
  @Getter private HttpClient httpClient;

  /**
   * If true, non-2xx response statuses will not throw an exception. The response body can be
   * checked for error details. Default is false, meaning that non-success statuses will throw a
   * {@link RestClientException}.
   */
  @Getter private boolean allowNonSuccessStatus;

  /**
   * Executes a GET request and returns the response body converted to the specified type.
   *
   * @param <T> the response type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param responseType the class to convert the response body to
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted response object
   * @throws RestClientException if the response status is not 2xx
   */
  public <T> T get(String path, Class<T> responseType, Object... pathParams) {
    RequestWrapper request = new RequestWrapper(requestBuilder(path, pathParams).GET().build());
    return execute(request, new ResponseType<>(responseType));
  }

  /**
   * Executes a GET request and returns the response body converted to a list.
   *
   * @param <T> the list element type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param typeRef the type reference for the response body
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted list
   * @throws RestClientException if the response status is not 2xx
   */
  public <T> T get(String path, JsonType<T> typeRef, Object... pathParams) {
    RequestWrapper request = new RequestWrapper(requestBuilder(path, pathParams).GET().build());
    return execute(request, new ResponseType<>(typeRef));
  }

  /**
   * Executes a GET request and returns the response body as a byte array.
   *
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the response body as a byte array
   * @throws RestClientException if the response status is not 2xx
   */
  public byte[] getAsByte(String path, Object... pathParams) {
    RequestWrapper request = new RequestWrapper(requestBuilder(path, pathParams).GET().build());
    return executeAsBytes(request);
  }

  /**
   * Executes a POST request and returns the response body converted to the specified type.
   *
   * @param <T> the response type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param requestBody the request body, serialized to JSON; {@code null} sends no body
   * @param responseType the class to convert the response body to
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted response object
   * @throws RestClientException if the response status is not 2xx
   */
  public <T> T post(String path, Object requestBody, Class<T> responseType, Object... pathParams) {
    byte[] body = bodyAsBytes(requestBody);
    RequestWrapper request =
        new RequestWrapper(
            requestBuilder(path, pathParams).POST(BodyPublishers.ofByteArray(body)).build(), body);
    return execute(request, new ResponseType<>(responseType));
  }

  /**
   * Executes a multipart/form-data POST request and returns the response body converted to the
   * specified type.
   *
   * <p>Each part value may be one of the following types:
   *
   * <ul>
   *   <li>{@link Path} &mdash; sent as a file
   *   <li>{@code byte[]} &mdash; sent as binary data
   *   <li>any other type &mdash; sent as text using {@code toString()}
   * </ul>
   *
   * @param <T> the response type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param parts a map of part names to part values
   * @param responseType the class to convert the response body to
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted response object
   * @throws RestClientException if the response status is not 2xx
   * @throws UncheckedIOException if reading a file part fails
   */
  public <T> T postMultipart(
      String path, Map<String, Object> parts, Class<T> responseType, Object... pathParams) {
    String boundary = UUID.randomUUID().toString();
    Map<String, String> requestHeaders = resolveHeaders();
    requestHeaders.put("Content-Type", "multipart/form-data; boundary=" + boundary);
    byte[] body = toMultipartBody(parts, boundary);
    RequestWrapper request =
        new RequestWrapper(
            requestBuilder(path, requestHeaders, pathParams)
                .POST(BodyPublishers.ofByteArray(body))
                .build(),
            body);
    return execute(request, new ResponseType<>(responseType));
  }

  /**
   * Executes a PUT request and returns the response body converted to the specified type.
   *
   * @param <T> the response type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param requestBody the request body, serialized to JSON; {@code null} sends no body
   * @param responseType the class to convert the response body to
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted response object
   * @throws RestClientException if the response status is not 2xx
   */
  public <T> T put(String path, Object requestBody, Class<T> responseType, Object... pathParams) {
    byte[] body = bodyAsBytes(requestBody);
    RequestWrapper request =
        new RequestWrapper(
            requestBuilder(path, pathParams).PUT(BodyPublishers.ofByteArray(body)).build(), body);
    return execute(request, new ResponseType<>(responseType));
  }

  /**
   * Executes a bodyless DELETE request and returns the response body converted to the specified
   * type.
   *
   * @param <T> the response type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param responseType the class to convert the response body to
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted response object
   * @throws RestClientException if the response status is not 2xx
   */
  public <T> T delete(
      String path, Object requestBody, Class<T> responseType, Object... pathParams) {
    byte[] body = bodyAsBytes(requestBody);
    RequestWrapper request =
        new RequestWrapper(
            requestBuilder(path, pathParams)
                .method("DELETE", BodyPublishers.ofByteArray(body))
                .build(),
            body);
    return execute(request, new ResponseType<>(responseType));
  }

  private HttpRequest.Builder requestBuilder(String path, Object... pathParams) {
    return requestBuilder(path, resolveHeaders(), pathParams);
  }

  private HttpRequest.Builder requestBuilder(
      String path, Map<String, String> requestHeaders, Object... pathParams) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder().uri(URI.create(resolvePath(path, pathParams)));
    requestHeaders.forEach(builder::header);
    return builder;
  }

  private byte[] bodyAsBytes(Object body) {
    return body == null ? new byte[0] : JsonUtils.obj2str(body).getBytes(StandardCharsets.UTF_8);
  }

  private Map<String, String> resolveHeaders() {
    Map<String, String> resolvedHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    headerSuppliers.forEach(
        (key, value) -> {
          String headerValue = value.get();
          if (headerValue != null && !headerValue.isEmpty()) {
            resolvedHeaders.put(key, headerValue);
          }
        });
    resolvedHeaders.putAll(headers);
    return resolvedHeaders;
  }

  private byte[] toMultipartBody(Map<String, Object> parts, String boundary) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (Map.Entry<String, Object> entry : parts.entrySet()) {
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        if (entry.getValue() instanceof Path filePath) {
          String mimeType = Files.probeContentType(filePath);
          if (mimeType == null) {
            mimeType = "application/octet-stream";
          }
          baos.write(
              (CONTENT_DISPOSITION_PREFIX
                      + entry.getKey()
                      + "\"; filename=\""
                      + filePath.getFileName()
                      + "\"\r\n")
                  .getBytes(StandardCharsets.UTF_8));
          baos.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
          baos.write(Files.readAllBytes(filePath));
        } else if (entry.getValue() instanceof byte[] bytes) {
          baos.write(
              (CONTENT_DISPOSITION_PREFIX
                      + entry.getKey()
                      + "\"; filename=\""
                      + entry.getKey()
                      + "\"\r\n")
                  .getBytes(StandardCharsets.UTF_8));
          baos.write(
              "Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
          baos.write(bytes);
        } else {
          baos.write(
              (CONTENT_DISPOSITION_PREFIX + entry.getKey() + "\"\r\n\r\n")
                  .getBytes(StandardCharsets.UTF_8));
          baos.write(entry.getValue().toString().getBytes(StandardCharsets.UTF_8));
        }
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
      }
      baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      return baos.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <T> T execute(RequestWrapper request, ResponseType<T> responseType) {
    ResponseWrapper<T> response = new ResponseWrapper<>(bodyHandler(responseType.getType()));
    send(request, response);
    handleResponse(response);
    return convertBody(response, responseType);
  }

  private byte[] executeAsBytes(RequestWrapper request) {
    ResponseWrapper<byte[]> response = new ResponseWrapper<>(bodyHandler(byte[].class));
    send(request, response);
    handleResponse(response);
    return response.getResponse().body();
  }

  @SuppressWarnings("unchecked")
  private <T> HttpResponse.BodyHandler<T> bodyHandler(Class<T> responseType) {
    return responseType == byte[].class
        ? (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofByteArray()
        : (HttpResponse.BodyHandler<T>) HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
  }

  @SuppressWarnings("unchecked")
  private <T> T convertBody(ResponseWrapper<T> response, ResponseType<T> responseType) {
    String body = response.bodyAsString();
    if (responseType.getJsonType() != null) {
      return JsonUtils.str2obj(body, responseType.getJsonType());
    }

    Class<T> type = responseType.getType();
    return switch (type) {
      case Class<?> target when target == HttpResponse.class -> (T) response.getResponse();
      case Class<?> target when target == Void.class || target == void.class -> null;
      case Class<?> target when target == String.class -> (T) body;
      case Class<?> target when target == Integer.class -> (T) Integer.valueOf(body.trim());
      case Class<?> target when target == Long.class -> (T) Long.valueOf(body.trim());
      case Class<?> target when target == UUID.class -> {
        String value = body.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1);
        }
        yield (T) UUID.fromString(value);
      }
      default -> JsonUtils.str2obj(body, type);
    };
  }

  private <T> void send(RequestWrapper request, ResponseWrapper<T> response) {
    new FilterContextImpl(filters, this::getHttpClientWithInit).next(request, response);
  }

  private synchronized HttpClient getHttpClientWithInit() {
    if (httpClient == null) {
      httpClient =
          HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMillis)).build();
    }
    return httpClient;
  }

  private void handleResponse(ResponseWrapper<?> response) {
    if (allowNonSuccessStatus) {
      return;
    }
    if (response.getResponse().statusCode() < 200 || response.getResponse().statusCode() >= 300) {
      throw new RestClientException(
          response.getResponse().statusCode(), Objects.toString(response.getResponse().body()));
    }
  }

  private String resolvePath(String path, Object... pathParams) {
    return resolvePath(baseUrl, path, pathParams);
  }

  String resolvePath(String baseUrl, String path, Object... pathParams) {
    String resolved = path;

    if (baseUrl != null) {
      if (baseUrl.endsWith("/") && resolved.startsWith("/")) {
        resolved = resolved.substring(1);
      } else if (!baseUrl.endsWith("/") && !resolved.startsWith("/")) {
        resolved = "/" + resolved;
      }
    }

    for (Object param : pathParams) {
      resolved = resolved.replaceFirst("\\{[^}]+\\}", param.toString());
    }

    return baseUrl + resolved;
  }

  @Data
  public static class ResponseType<T> {
    private Class<T> type;
    private JsonType<T> jsonType;

    ResponseType(Class<T> type) {
      this.type = type;
    }

    ResponseType(JsonType<T> jsonType) {
      this.jsonType = jsonType;
    }
  }

  public static class RestClientBuilder {
    private boolean defaultHeaders = true;

    public RestClientBuilder() {
      if (defaultHeaders) {
        this.header("Content-Type", "application/json; charset=UTF-8");
        this.header("Accept", "*/*");
        this.header("Accept-Language", Locale.getDefault().toString().replace("_", "-"));
      }
    }

    /**
     * Sets whether to include default headers (Content-Type, Accept, Accept-Language).
     *
     * @param defaultHeaders true to include default headers, false to exclude them
     * @return this builder instance for chaining
     */
    public RestClientBuilder defaultHeaders(boolean defaultHeaders) {
      this.defaultHeaders = defaultHeaders;
      return this;
    }

    /**
     * Configures the base URL from Quarkus configuration properties:
     *
     * <ul>
     *   <li>quarkus.http.host (default: localhost)
     *   <li>quarkus.http.port (default: 8080)
     *   <li>quarkus.rest.path (default: "")
     * </ul>
     *
     * The final base URL is constructed as "http://{host}:{port}{restPath}". This is useful for
     * testing against a Quarkus application running with the default HTTP configuration.
     *
     * @return this builder instance for chaining
     */
    public RestClientBuilder quarkus() {
      Config config = ConfigProvider.getConfig();
      String host = config.getOptionalValue("quarkus.http.host", String.class).orElse("localhost");
      int port = config.getOptionalValue("quarkus.http.port", Integer.class).orElse(8080);
      String restPath = config.getOptionalValue("quarkus.rest.path", String.class).orElse("/");
      baseUrl("http://" + host + ":" + port + restPath);

      return this;
    }
  }
}
