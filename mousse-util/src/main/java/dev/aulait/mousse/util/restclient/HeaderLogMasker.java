package dev.aulait.mousse.util.restclient;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

final class HeaderLogMasker {

  private final Set<String> maskedHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

  HeaderLogMasker() {
    ConfigProviderResolver.instance()
      .getConfig()
      .getOptionalValue("mousse.rest-client.logging.masked-headers", String[].class)
      .ifPresent(this::configMaskedHeaders);
  }

  private void configMaskedHeaders(String[] configuredHeaders) {
    if (configuredHeaders.length <= 0) {
      return;
    }
    Arrays.stream(configuredHeaders).forEach(maskedHeaders::add);
  }

  Map<String, List<String>> mask(Map<String, List<String>> headers) {
    Map<String, List<String>> loggedHeaders = new LinkedHashMap<>();
    headers.forEach(
        (name, values) ->
            loggedHeaders.put(name, maskedHeaders.contains(name) ? List.of("<hidden>") : values));
    return loggedHeaders;
  }
}
