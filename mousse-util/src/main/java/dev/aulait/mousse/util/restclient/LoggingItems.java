package dev.aulait.mousse.util.restclient;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

abstract class LoggingItems<E extends Enum<E>> {

  private final Class<E> itemType;
  private final Set<E> items;

  LoggingItems(Class<E> itemType, String configProperty) {
    this.itemType = itemType;
    items = EnumSet.allOf(itemType);
    ConfigProviderResolver.instance()
        .getConfig()
        .getOptionalValue(configProperty, String[].class)
        .ifPresent(this::configLoggingItems);
  }

  private void configLoggingItems(String[] configuredItems) {
    if (configuredItems.length <= 0) {
      return;
    }
    items.clear();
    if (configuredItems.length == 1 && "none".equalsIgnoreCase(configuredItems[0])) {
      return;
    }
    Arrays.stream(configuredItems)
        .map(item -> Enum.valueOf(itemType, item.toUpperCase(Locale.ROOT)))
        .forEach(items::add);
  }

  boolean includes(E item) {
    return items.contains(item);
  }
}
