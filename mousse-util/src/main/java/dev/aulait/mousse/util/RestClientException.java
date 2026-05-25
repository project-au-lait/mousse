package dev.aulait.mousse.util;

public class RestClientException extends RuntimeException {

  private final int statusCode;
  private final String body;

  public RestClientException(Throwable cause) {
    super(cause);
    this.statusCode = -1;
    this.body = null;
  }

  public RestClientException(int statusCode, String body) {
    super("HTTP error: statusCode=" + statusCode + ", body=" + body);
    this.statusCode = statusCode;
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getBody() {
    return body;
  }
}
