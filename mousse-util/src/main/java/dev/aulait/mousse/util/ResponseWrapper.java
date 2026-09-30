package dev.aulait.mousse.util;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.Data;

@Data
public class ResponseWrapper<T> {
  private final HttpResponse.BodyHandler<T> bodyHandler;

  private HttpResponse<T> response;
  private String plainBody;

  ResponseWrapper(HttpResponse.BodyHandler<T> bodyHandler) {
    this.bodyHandler = bodyHandler;
  }

  public String bodyAsString() {
    if (plainBody != null) {
      return plainBody;
    }

    Object body = response.body();
    plainBody =
        body instanceof byte[] bytes
            ? new String(bytes, StandardCharsets.UTF_8)
            : Objects.toString(body, "");
    return plainBody;
  }
}
