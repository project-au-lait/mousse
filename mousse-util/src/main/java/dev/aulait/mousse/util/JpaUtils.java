package dev.aulait.mousse.util;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JpaUtils {

  /**
   * Propagates the {@link jakarta.persistence.Id} value of the given JPA entity to the
   * corresponding field in the {@link jakarta.persistence.EmbeddedId} of each child entity
   * reachable via {@link jakarta.persistence.OneToMany} fields. The target field in the composite
   * key is resolved by converting the {@link jakarta.persistence.JoinColumn#name()} from
   * snake_case to camelCase. If a child's composite ID is {@code null}, a new instance is created
   * and assigned before the value is set.
   *
   * @param parent the JPA entity whose ID is propagated to its children
   */
  public static void propergateId(Object parent) {
    try {
      PropergationContext ctx = buildContext(parent);
      for (OneToManyFieldInfo info : ctx.getOneToManyFieldInfos()) {
        applyToChildren(ctx.getParentIdValue(), info, parent);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("java:S3011")
  private static PropergationContext buildContext(Object parent) throws IllegalAccessException {
    Class<?> parentClass = parent.getClass();

    Field idField =
        Arrays.stream(parentClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(Id.class))
            .findFirst()
            .orElseThrow();
    idField.setAccessible(true);

    String parentIdFieldNameInChild =
        Arrays.stream(parentClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(OneToMany.class))
            .map(f -> f.getAnnotation(JoinColumn.class))
            .map(jc -> snakeToCamelCase(jc.name()))
            .findFirst()
            .orElseThrow();

    List<OneToManyFieldInfo> infos =
        Arrays.stream(parentClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(OneToMany.class))
            .map(f -> buildOneToManyFieldInfo(f, parentIdFieldNameInChild))
            .toList();

    PropergationContext ctx = new PropergationContext();
    ctx.setParentIdValue(idField.get(parent));
    ctx.setParentIdFieldNameInChild(parentIdFieldNameInChild);
    ctx.setOneToManyFieldInfos(infos);
    return ctx;
  }

  @SuppressWarnings("java:S3011")
  private static OneToManyFieldInfo buildOneToManyFieldInfo(
      Field oneToManyField, String parentIdFieldNameInChild) {
    oneToManyField.setAccessible(true);

    ParameterizedType genericType = (ParameterizedType) oneToManyField.getGenericType();
    Class<?> childClass = (Class<?>) genericType.getActualTypeArguments()[0];

    Field embeddedIdField =
        Arrays.stream(childClass.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(EmbeddedId.class))
            .findFirst()
            .orElseThrow();
    embeddedIdField.setAccessible(true);

    Field compositeIdField =
        Arrays.stream(embeddedIdField.getType().getDeclaredFields())
            .filter(f -> f.getName().equals(parentIdFieldNameInChild))
            .findFirst()
            .orElseThrow();
    compositeIdField.setAccessible(true);

    OneToManyFieldInfo info = new OneToManyFieldInfo();
    info.setOneToManyField(oneToManyField);
    info.setChildClass(childClass);
    info.setEmbeddedIdField(embeddedIdField);
    info.setCompositeIdField(compositeIdField);
    return info;
  }

  @SuppressWarnings("java:S3011")
  private static void applyToChildren(Object parentIdValue, OneToManyFieldInfo info, Object parent)
      throws ReflectiveOperationException {

    Collection<?> children = (Collection<?>) info.getOneToManyField().get(parent);
    if (children != null) {
      for (Object child : children) {
        Object compositeId = info.getEmbeddedIdField().get(child);
        if (compositeId == null) {
          compositeId = info.getEmbeddedIdField().getType().getDeclaredConstructor().newInstance();
          info.getEmbeddedIdField().set(child, compositeId);
        }
        info.getCompositeIdField().set(compositeId, parentIdValue);
      }
    }
  }

  private static String snakeToCamelCase(String snake) {
    String[] parts = snake.split("_");
    StringBuilder camel = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      camel.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
    }
    return camel.toString();
  }

  @Data
  static class PropergationContext {
    private Object parentIdValue;
    private String parentIdFieldNameInChild;
    private List<OneToManyFieldInfo> oneToManyFieldInfos;
  }

  @Data
  static class OneToManyFieldInfo {
    private Field oneToManyField;
    private Class<?> childClass;
    private Field embeddedIdField;
    private Field compositeIdField;
  }
}
