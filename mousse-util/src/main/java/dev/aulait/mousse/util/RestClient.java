package dev.aulait.mousse.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.Builder;
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
  @Getter @Singular private Map<String, String> headers;
  @Getter @Singular private Map<String, Supplier<String>> headerSuppliers;
  @Getter @Builder.Default private HttpClient httpClient = HttpClient.newHttpClient();

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
    HttpRequest request = newRequest(resolvePath(path, pathParams)).GET().build();
    return convertResponse(executeAsString(request), responseType);
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
  public <T> List<T> getAsList(String path, JsonType<List<T>> typeRef, Object... pathParams) {
    HttpRequest request = newRequest(resolvePath(path, pathParams)).GET().build();
    return JsonUtils.str2obj(executeAsString(request), typeRef);
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
    HttpRequest request = newRequest(resolvePath(path, pathParams)).GET().build();
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
    HttpRequest request =
        newRequest(resolvePath(path, pathParams)).POST(toBodyPublisher(requestBody)).build();
    return convertResponse(executeAsString(request), responseType);
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
    HttpRequest.Builder builder = newRequest(resolvePath(path, pathParams));
    builder.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    HttpRequest request = builder.POST(toMultipartBodyPublisher(parts, boundary)).build();
    return convertResponse(executeAsString(request), responseType);
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
    HttpRequest request =
        newRequest(resolvePath(path, pathParams)).PUT(toBodyPublisher(requestBody)).build();
    return convertResponse(executeAsString(request), responseType);
  }

  /**
   * Executes a DELETE request and returns the response body converted to the specified type.
   *
   * @param <T> the response type
   * @param path the request path; path parameters are specified in {@code {name}} format
   * @param requestBody the request body, serialized to JSON; {@code null} sends no body
   * @param responseType the class to convert the response body to
   * @param pathParams values for path parameters, substituted in order of appearance
   * @return the converted response object
   * @throws RestClientException if the response status is not 2xx
   */
  public <T> T delete(
      String path, Object requestBody, Class<T> responseType, Object... pathParams) {
    HttpRequest request =
        newRequest(resolvePath(path, pathParams))
            .method("DELETE", toBodyPublisher(requestBody))
            .build();
    return convertResponse(executeAsString(request), responseType);
  }

  private HttpRequest.Builder newRequest(String url) {
    HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
    headerSuppliers.forEach(
        (key, value) -> {
          String v = value.get();
          if (v != null && !v.isEmpty()) {
            builder.header(key, v);
          }
        });
    headers.forEach(builder::header);
    return builder;
  }

  private BodyPublisher toBodyPublisher(Object body) {
    if (body == null) {
      return BodyPublishers.noBody();
    }
    return BodyPublishers.ofString(JsonUtils.obj2str(body), StandardCharsets.UTF_8);
  }

  private BodyPublisher toMultipartBodyPublisher(Map<String, Object> parts, String boundary) {
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
      return BodyPublishers.ofByteArray(baos.toByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String executeAsString(HttpRequest request) {
    HttpResponse<String> response =
        execute(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    handleResponse(response);
    return response.body();
  }

  private byte[] executeAsBytes(HttpRequest request) {
    HttpResponse<byte[]> response = execute(request, HttpResponse.BodyHandlers.ofByteArray());
    handleResponse(response);
    return response.body();
  }

  private <T> HttpResponse<T> execute(
      HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
    try {
      log.debug("{} {}", request.method(), request.uri());

      HttpResponse<T> response = httpClient.send(request, bodyHandler);

      log.debug("Status: {}", response.statusCode());

      return response;
    } catch (IOException e) {
      throw new RestClientException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RestClientException(e);
    }
  }

  private void handleResponse(HttpResponse<?> response) {
    if (allowNonSuccessStatus) {
      return;
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new RestClientException(response.statusCode(), Objects.toString(response.body()));
    }
  }

  @SuppressWarnings("unchecked")
  <T> T convertResponse(String body, Class<T> responseType) {
    if (responseType == Void.class || responseType == void.class) {
      return null;
    } else if (responseType == String.class) {
      return (T) body;
    } else if (responseType == Integer.class) {
      return (T) Integer.valueOf(body.trim());
    } else if (responseType == Long.class) {
      return (T) Long.valueOf(body.trim());
    } else if (responseType == UUID.class) {
      String value = body.trim();
      if (value.startsWith("\"") && value.endsWith("\"")) {
        value = value.substring(1, value.length() - 1);
      }
      return (T) UUID.fromString(value);
    } else {
      return JsonUtils.str2obj(body, responseType);
    }
  }

  String resolvePath(String path, Object... pathParams) {
    String resolved = path;

    if (baseUrl != null && baseUrl.endsWith("/") && resolved.startsWith("/")) {
      resolved = resolved.substring(1);
    }

    for (Object param : pathParams) {
      resolved = resolved.replaceFirst("\\{[^}]+\\}", param.toString());
    }

    return baseUrl + resolved;
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
