package dev.aulait.mousse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.aulait.mousse.util.BeanUtils.BeanType;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

class BeanUtilsTests {

  @Test
  void collectionMapTest() {
    BeanX beanX = BeanX.of("id", "name");
    List<BeanX> src = List.of(beanX);
    Set<BeanY> dst = BeanUtils.map(src, new BeanType<Set<BeanY>>() {});

    assertEquals(1, dst.size());
    BeanY beanY = dst.iterator().next();
    assertEquals(beanX.getId(), beanY.getId());
    assertEquals(beanX.getName(), beanY.getName());
  }

  @Test
  void typeMapWithSkipTest() {
    String typeMapName = "BeanXToBeanY";
    BeanUtils.registerTypeMap(BeanX.class, BeanY.class, typeMapName)
        .addMappings(mapper -> mapper.skip(BeanY::setName))
        .implicitMappings();

    BeanX src = BeanX.of("id", "name");
    BeanY dst = BeanUtils.map(src, BeanY.class, typeMapName);

    assertEquals(src.getId(), dst.getId());
    assertEquals(null, dst.getName());
  }

  @AllArgsConstructor
  @Builder
  @Data
  @NoArgsConstructor
  static class BeanX {
    String id;
    String name;

    static BeanX of(String id, String name) {
      return BeanX.builder().id(id).name(name).build();
    }
  }

  @AllArgsConstructor
  @Builder
  @Data
  @NoArgsConstructor
  static class BeanY {
    String id;
    String name;

    static BeanY of(String id, String name) {
      return BeanY.builder().id(id).name(name).build();
    }
  }
}
