package dev.aulait.mousse.util.exec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecUtilsTests {

  @Nested
  class ExecTests {

    @Test
    void commandReturnsExitCodeAndOutputTest() {
      ExecResult result = ExecUtils.exec("echo plain-output");

      assertEquals(0, result.getExitCode());
      assertTrue(result.getStdout().endsWith("plain-output"));
    }

    @Test
    void substitutionMapReplacesCommandValueTest() {
      ExecResult result =
          ExecUtils.exec("echo ${value}", Map.of("value", "substituted-output"));

      assertTrue(result.getStdout().endsWith("substituted-output"));
    }

    @Test
    void workingDirectoryRunsCommandThereTest(@TempDir Path workingDirectory)
        throws IOException {
      ExecResult result =
          ExecUtils.exec(workingDirectoryCommand(), Map.of(), workingDirectory);

      assertTrue(result.getStdout().endsWith(workingDirectory.toRealPath().toString()));
    }

    @Test
    void environmentPassesVariablesToCommandTest() {
      ExecResult result =
          ExecUtils.exec(
              environmentValueCommand(), Map.of(), Map.of("MOUSSE_EXEC_TEST", "environment-output"));

      assertTrue(result.getStdout().endsWith("environment-output"));
    }

    @Test
    void paramUsesConfiguredCommandAndSubstitutionsTest() {
      ExecParam param =
          ExecParam.builder()
              .command("echo ${value}")
              .substitutionMap(Map.of("value", "parameter-output"))
              .build();

      ExecResult result = ExecUtils.exec(param);

      assertTrue(result.getStdout().endsWith("parameter-output"));
    }
  }

  @Nested
  class ExecWithStdoutTests {

    @Test
    void returnsCommandOutputTest() {
      String output =
          ExecUtils.execWithStdout("echo ${value}", Map.of("value", "stdout-output"));

      assertTrue(output.endsWith("stdout-output"));
    }

    @Test
    void workingDirectoryRunsCommandThereTest(@TempDir Path workingDirectory)
        throws IOException {
      String output =
          ExecUtils.execWithStdout(workingDirectoryCommand(), Map.of(), workingDirectory);

      assertTrue(output.endsWith(workingDirectory.toRealPath().toString()));
    }
  }

  @Nested
  class ExecWithExitCodeTests {

    @Test
    void returnsSuccessfulExitCodeTest() {
      int exitCode = ExecUtils.execWithExitCode("echo success", Map.of());

      assertEquals(0, exitCode);
    }

    @Test
    void environmentPassesVariablesToCommandTest() {
      int exitCode =
          ExecUtils.execWithExitCode(
              environmentCheckCommand(), Map.of(), Map.of("MOUSSE_EXEC_TEST", "expected-value"));

      assertEquals(0, exitCode);
    }
  }

  @Nested
  class FailureTests {

    @Test
    void nonZeroExitIncludesCapturedStandardOutputAndErrorTest() {
      UncheckedIOException exception =
          assertThrows(
              UncheckedIOException.class,
              () -> ExecUtils.exec(commandWithOutputAndNonZeroExit()));

      assertTrue(exception.getMessage().contains("exec-stdout"));
      assertTrue(exception.getMessage().contains("exec-stderr"));
    }

    @Test
    void missingWorkingDirectoryThrowsUncheckedIOExceptionTest(@TempDir Path tempDirectory) {
      Path missingDirectory = tempDirectory.resolve("missing");

      assertThrows(
          UncheckedIOException.class,
          () -> ExecUtils.exec("echo unreachable", Map.of(), missingDirectory));
    }
  }

  private static String environmentValueCommand() {
    return SystemUtils.IS_OS_WINDOWS ? "echo %MOUSSE_EXEC_TEST%" : "echo $MOUSSE_EXEC_TEST";
  }

  private static String environmentCheckCommand() {
    return SystemUtils.IS_OS_WINDOWS
        ? "if \"%MOUSSE_EXEC_TEST%\"==\"expected-value\" (exit /b 0) else (exit /b 1)"
        : "[ \"$MOUSSE_EXEC_TEST\" = \"expected-value\" ]";
  }

  private static String workingDirectoryCommand() {
    return SystemUtils.IS_OS_WINDOWS ? "cd" : "pwd";
  }

  private static String commandWithOutputAndNonZeroExit() {
    return SystemUtils.IS_OS_WINDOWS
        ? "echo exec-stdout & echo exec-stderr 1>&2 & exit /b 9"
        : "echo exec-stdout; echo exec-stderr >&2; exit 9";
  }
}
