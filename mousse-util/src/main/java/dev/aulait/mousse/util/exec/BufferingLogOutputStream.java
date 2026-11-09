package dev.aulait.mousse.util.exec;

import lombok.RequiredArgsConstructor;
import org.apache.commons.exec.LogOutputStream;
import org.slf4j.Logger;

@RequiredArgsConstructor
class BufferingLogOutputStream extends LogOutputStream {
  private final Logger log;
  private StringBuffer buffer = new StringBuffer();

  @Override
  protected void processLine(String line, int logLevel) {
    log.info(line);
    buffer.append(line).append(System.lineSeparator());
  }

  @Override
  public String toString() {
    return buffer.toString();
  }
}
