package dev.aulait.mousse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import java.util.Set;
import lombok.Data;
import org.junit.jupiter.api.Test;

class JpaUtilsTests {
  @Test
  void testPropergateId() {
    var east = new East();
    east.setId(1L);

    var west = new West();
    west.setId(2L);

    var bridge = new EastWestBridge();
    bridge.setWest(west);

    east.setBridges(Set.of(bridge));

    JpaUtils.propergateId(east);

    assertEquals(east.getId(), bridge.getId().getEastId());
  }

  @Data
  static class East {
    @Id private Long id;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(
        name = "east_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
    private Set<EastWestBridge> bridges;
  }

  @Data
  static class West {
    @Id private Long id;
  }

  @Data
  static class EastWestBridge {
    @EmbeddedId private EastWestBridgeId id;

    @MapsId("westId")
    @ManyToOne(fetch = FetchType.LAZY)
    private West west;
  }

  @Data
  @Embeddable
  static class EastWestBridgeId {
    private Long eastId;
    private Long westId;
  }
}
