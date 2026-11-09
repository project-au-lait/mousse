package dev.aulait.mousse.util.exec;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Builder
@RequiredArgsConstructor
@Value
public class ExecResult {
  private final int exitCode;
  private final String stdout;
  private final String stderr;
}
