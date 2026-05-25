package dev.aulait.mousse.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for JSON serialization and deserialization using Jackson.
 *
 * <p>This class provides static methods to convert objects to JSON strings and vice versa, as well
 * as reading from and writing to files.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    // Configure the ObjectMapper as per Quarkus defaults
    // https://quarkus.io/guides/rest-json#jackson
    // TODO make this configurable
    MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MAPPER.findAndRegisterModules();
  }

  /**
   * Configures the ObjectMapper using the provided configurator.
   *
   * @param configurator the configurator to customize the ObjectMapper
   */
  public static void configure(Consumer<ObjectMapper> configurator) {
    configurator.accept(MAPPER);
  }

  /**
   * Converts an object to its JSON string representation.
   *
   * @param obj the object to be converted to JSON
   * @return the JSON string representation of the object
   */
  public static String obj2str(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Converts an object to a formatted (pretty-printed) JSON string representation.
   *
   * @param object the object to be converted to a formatted JSON string
   * @return the formatted JSON string representation of the object
   */
  public static String obj2fmtstr(Object object) {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Reads a JSON file and converts its content to an object of the specified class.
   *
   * @param <T> the type of the object to be returned
   * @param filePath the path to the JSON file
   * @param valueType the class of the object to be returned
   * @return an object of the specified class with data read from the JSON file
   */
  public static <T> T file2obj(Path filePath, Class<T> valueType) {
    try {
      return MAPPER.readValue(filePath.toFile(), valueType);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Converts a JSON string to an object of the specified class.
   *
   * @param <T> the type of the object to be returned
   * @param str the JSON string to be converted
   * @param valueType the class of the object to be returned
   * @return an object of the specified class with data read from the JSON string
   */
  public static <T> T str2obj(String str, Class<T> valueType) {
    try {
      return MAPPER.readValue(str, valueType);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Converts a JSON string to an object of the type specified by a TypeReference.
   *
   * @param <T> the type of the object to be returned
   * @param str the JSON string to be converted
   * @param typeRef the TypeReference describing the target type
   * @return an object of the specified type with data read from the JSON string
   */
  public static <T> T str2obj(String str, JsonType<T> typeRef) {
    try {
      return MAPPER.readValue(str, typeRef);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
