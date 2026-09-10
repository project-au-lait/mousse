package dev.aulait.mousse.util;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class FilterContextImpl implements FilterContext {

  private final List<RestClientFilter> filters;
  private final Supplier<HttpClient> httpClientSupplier;
  private int index;

  FilterContextImpl(List<RestClientFilter> filters, Supplier<HttpClient> httpClientSupplier) {
    this.filters = filters;
    this.httpClientSupplier = httpClientSupplier;
  }

  @Override
  public <T> HttpResponse<T> next(
      RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
    if (index < filters.size()) {
      return filters.get(index++).filter(request, bodyHandler, this);
    }
    return sendRequest(request, bodyHandler);
  }

  private <T> HttpResponse<T> sendRequest(
      RestClientRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
    try {
      log.debug("{} {}", request.method(), request.uri());
      HttpResponse<T> response = httpClientSupplier.get().send(request.request(), bodyHandler);
      log.debug("Status: {}", response.statusCode());
      return response;
    } catch (IOException e) {
      throw new RestClientException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RestClientException(e);
    }
  }
}
