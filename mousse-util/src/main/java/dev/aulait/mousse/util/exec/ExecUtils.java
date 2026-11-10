package dev.aulait.mousse.util.exec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang3.SystemUtils;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecUtils {

  /**
   * Executes a command and returns its standard output.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @return the command's standard output, with leading and trailing whitespace removed
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static String execWithStdout(String command, Map<String, ?> substitutionMap) {
    return exec(command, substitutionMap).getStdout();
  }

  /**
   * Executes a command in the specified working directory and returns its standard output.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @param workingDirectory the directory in which to execute the command
   * @return the command's standard output, with leading and trailing whitespace removed
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static String execWithStdout(
      String command, Map<String, ?> substitutionMap, Path workingDirectory) {
    return exec(command, substitutionMap, workingDirectory).getStdout();
  }

  /**
   * Executes a command and returns its exit code.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @return the command's exit code
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static int execWithExitCode(String command, Map<String, ?> substitutionMap) {
    return exec(command, substitutionMap).getExitCode();
  }

  /**
   * Executes a command with the specified environment variables and returns its exit code.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @param environment environment variables to add or override for the command
   * @return the command's exit code
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static int execWithExitCode(
      String command, Map<String, ?> substitutionMap, Map<String, String> environment) {
    return exec(command, substitutionMap, environment).getExitCode();
  }

  /**
   * Executes a command without command-argument substitutions.
   *
   * @param command the command to execute
   * @return the command's exit code and captured standard output and error
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static ExecResult exec(String command) {
    return exec(command, Map.of());
  }

  /**
   * Executes a command using the supplied command-argument substitutions.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @return the command's exit code and captured standard output and error
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static ExecResult exec(String command, Map<String, ?> substitutionMap) {
    return exec(ExecParam.builder().command(command).substitutionMap(substitutionMap).build());
  }

  /**
   * Executes a command in the specified working directory using the supplied substitutions.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @param workingDirectory the directory in which to execute the command
   * @return the command's exit code and captured standard output and error
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static ExecResult exec(
      String command, Map<String, ?> substitutionMap, Path workingDirectory) {
    return exec(
        ExecParam.builder()
            .command(command)
            .substitutionMap(substitutionMap)
            .workingDirectory(workingDirectory)
            .build());
  }

  /**
   * Executes a command with the supplied substitutions and environment variables.
   *
   * @param command the command to execute
   * @param substitutionMap values used to substitute command arguments
   * @param environment environment variables to add or override for the command
   * @return the command's exit code and captured standard output and error
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static ExecResult exec(
      String command, Map<String, ?> substitutionMap, Map<String, String> environment) {
    return exec(
        ExecParam.builder()
            .command(command)
            .substitutionMap(substitutionMap)
            .environment(environment)
            .build());
  }

  /**
   * Executes a command using the supplied execution parameters.
   *
   * @param param the command, substitutions, working directory, and environment to use
   * @return the command's exit code and captured standard output and error
   * @throws UncheckedIOException if the command cannot be executed or exits with a non-zero status
   */
  public static ExecResult exec(ExecParam param) {
    CommandLine cmdLine = parse(param.getCommand());
    cmdLine.setSubstitutionMap(param.getSubstitutionMap());

    log.info("Executing command: {} at {}", cmdLine.toString(), param.getWorkingDirectory());

    var stdout = new BufferingLogOutputStream(log);
    var stderr = new BufferingLogOutputStream(log);
    PumpStreamHandler psh = new PumpStreamHandler(stdout, stderr);

    DefaultExecutor executor =
        DefaultExecutor.builder()
            .setExecuteStreamHandler(psh)
            .setWorkingDirectory(param.getWorkingDirectory().toFile())
            .get();

    Map<String, String> env = new HashMap<>(System.getenv());
    env.putAll(param.getEnvironment());

    int exitCodeLocal;
    try {
      exitCodeLocal = executor.execute(cmdLine, env);
    } catch (ExecuteException e) {
      String message =
          String.join(
              System.lineSeparator(), "stdout:", stdout.toString(), "stderr:", stderr.toString());
      throw new UncheckedIOException(message, e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return new ExecResult(exitCodeLocal, stdout.toString().trim(), stderr.toString().trim());
  }

  /**
   * Parses a command using the OS-specific shell, with a non-interactive login shell on macOS and
   * Linux.
   *
   * @param command the command to parse
   * @return the parsed CommandLine
   */
  private static CommandLine parse(String command) {
    if (SystemUtils.IS_OS_WINDOWS) {
      return new CommandLine("cmd").addArgument("/c").addArgument(command, false);
    } else if (SystemUtils.IS_OS_MAC) {
      return new CommandLine("zsh").addArgument("-lc").addArgument(command, false);
    } else if (SystemUtils.IS_OS_LINUX) {
      return new CommandLine("bash").addArgument("-lc").addArgument(command, false);
    }
    return new CommandLine(command);
  }
}
