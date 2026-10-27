package dev.aulait.mousse.util.restclient;


final class ResponseLoggingItems extends LoggingItems<ResponseLoggingItems.Item> {

  enum Item {
    STATUS,
    HEADER,
    BODY
  }

  ResponseLoggingItems() {
    super(Item.class, "mousse.rest-client.logging.response.items");
  }
}
