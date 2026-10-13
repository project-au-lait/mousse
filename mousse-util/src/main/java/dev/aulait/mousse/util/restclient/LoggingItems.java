package dev.aulait.mousse.util.restclient;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

final class LoggingItems {

  enum Item {
    STATUS,
    HEADER,
    BODY,
    METHOD,
    URI
  }

  private final Set<Item> items = EnumSet.allOf(Item.class);

  LoggingItems() {
    ConfigProviderResolver.instance()
      .getConfig()
      .getOptionalValue("mousse.rest-client.logging.items", String[].class)
      .ifPresent(this::configLoggingItems);
  }

  private void configLoggingItems(String[] configuredItems) {
    if (configuredItems.length <= 0) {
      return;
    }
    items.clear();
    Arrays.stream(configuredItems)
        .map(item -> Item.valueOf(item.toUpperCase(Locale.ROOT)))
        .forEach(items::add);
  }

  boolean includes(Item item) {
    return items.contains(item);
  }
}
