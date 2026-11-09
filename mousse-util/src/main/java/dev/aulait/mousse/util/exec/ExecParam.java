package dev.aulait.mousse.util.exec;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ExecParam {
  private String command;
  @Builder.Default private Map<String, ?> substitutionMap = new HashMap<>();
  @Builder.Default private Path workingDirectory = Path.of("");
  @Builder.Default private Map<String, String> environment = new HashMap<>();

  public Path getWorkingDirectory() {
    return workingDirectory.toAbsolutePath().normalize();
  }
}
