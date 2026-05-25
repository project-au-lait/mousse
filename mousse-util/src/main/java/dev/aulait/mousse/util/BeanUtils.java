package dev.aulait.mousse.util;

import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.TypeToken;

/**
 * Utility class for mapping Java beans using ModelMapper.
 *
 * <p>This class provides static methods to map objects between different types, register type maps,
 * and create type tokens for generic types.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanUtils {

  private static final ModelMapper MAPPER = new ModelMapper();

  /**
   * Configures the ModelMapper instance using the provided configurator.
   *
   * @param configurator a Consumer that accepts a ModelMapper instance for configuration
   */
  public static void configure(Consumer<ModelMapper> configurator) {
    configurator.accept(MAPPER);
  }

  /**
   * Maps the source object to an instance of the specified destination type.
   *
   * @param <T> the type of the destination object
   * @param src the source object to be mapped
   * @param dstType the class of the destination type
   *     <p>if used with generic types, use {@link #map(Object, BeanType)} instead
   * @return an instance of the destination type with the mapped values from the source object
   */
  public static <T> T map(Object src, Class<T> dstType) {
    return MAPPER.map(src, dstType);
  }

  /**
   * Maps the source object to an instance of the specified destination type using a named type map.
   *
   * <pre>{@code
   * // Example:
   * BeanX src = BeanX.of("id", "name");
   *
   * BeanUtils.registerTypeMap(BeanX.class, BeanY.class, "BeanXToBeanY")
   *  .addMappings(mapper -> mapper.skip(BeanY::setName))
   *  .implicitMappings();
   *
   * BeanY dst = BeanUtils.map(src, BeanY.class, "BeanXToBeanY");
   *
   * // Results:
   * // dst.getId() == "id"
   * // dst.getName() == null
   *
   * }</pre>
   *
   * @param <T> the type of the destination object
   * @param src the source object to be mapped
   * @param dstType the class of the destination type
   * @param typeMapName the name of the type map to be used for mapping
   * @return an instance of the destination type with the mapped values from the source object
   */
  public static <T> T map(Object src, Class<T> dstType, String typeMapName) {
    return MAPPER.map(src, dstType, typeMapName);
  }

  /**
   * Maps the source object to an instance of the specified destination type using a BeanType.
   *
   * <pre>{@code
   * // Example:
   * Set<BeanX> src = Set.of(BeanX.of("id", "name"));
   * List<BeanY> dst = BeanUtils.map(src, new BeanType<List<BeanY>>() {});
   *
   * // Results:
   * // dst.get(0).getId() == "id"
   * // dst.get(0).getName() == "name"
   *
   * }</pre>
   *
   * @param <T> the type of the destination object
   * @param src the source object to be mapped
   * @param dstType the BeanType representing the destination type
   * @return an instance of the destination type with the mapped values from the source object
   */
  public static <T> T map(Object src, BeanType<T> dstType) {
    return MAPPER.map(src, dstType.getType());
  }

  /**
   * Maps the source object to an instance of the specified destination type using a BeanType and a
   * named type map.
   *
   * @param <T> the type of the destination object
   * @param src the source object to be mapped
   * @param dstType the BeanType representing the destination type
   * @param typeMapName the name of the type map to be used for mapping
   * @return an instance of the destination type with the mapped values from the source object
   */
  public static <T> T map(Object src, BeanType<T> dstType, String typeMapName) {
    return MAPPER.map(src, dstType.getType(), typeMapName);
  }

  /**
   * Registers a type map between the source and destination types with the specified name.
   *
   * <pre>{@code
   * // Example:
   * TypeMap<BeanX, BeanY> typeMap = BeanUtils.registerTypeMap(BeanX.class, BeanY.class, "BeanXToBeanY");
   * typeMap.addMappings(mapper -> mapper.skip(BeanY::setName)).implicitMappings();
   *
   * BeanX src = BeanX.of("id", "name");
   * BeanY dst = BeanUtils.map(src, BeanY.class, "BeanXToBeanY");
   *
   * // Results:
   * // dst.getId() == "id"
   * // dst.getName() == null
   * }</pre>
   *
   * @param <S> the type of the source object
   * @param <D> the type of the destination object
   * @param srcType the class of the source type
   * @param dstType the class of the destination type
   * @param typeMapName the name of the type map to be registered
   * @return the registered TypeMap instance
   */
  public static <S, D> TypeMap<S, D> registerTypeMap(
      Class<S> srcType, Class<D> dstType, String typeMapName) {
    return MAPPER.emptyTypeMap(srcType, dstType, typeMapName);
  }

  /** Creates a type map between the source and destination types with the specified name. */
  public static class BeanType<T> extends TypeToken<T> {}
}
