package dev.aulait.mousse.util.restclient;

final class RequestLoggingItems extends LoggingItems<RequestLoggingItems.Item> {

  enum Item {
    METHOD,
    URI,
    HEADER,
    BODY
  }

  RequestLoggingItems() {
    super(Item.class, "mousse.rest-client.logging.request.items");
  }
}
