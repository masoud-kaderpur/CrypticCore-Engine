package at.tuwien.crypticcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExecutorTest {

  @TempDir
  Path tempDir;

  private Tracer tracer;

  @BeforeEach
  void setUp() {
    tracer = OpenTelemetry.noop().getTracer("test-tracer");
  }

  @Test
  @DisplayName("Constructor invocation for 100% class coverage")
  void testConstructor() {
    Executor executor = new Executor();
    assertThat(executor).isNotNull();
  }

  @Test
  @DisplayName("Happy Path: executes encryption successfully, moves temp file, and wipes key")
  void testExecuteSuccess() throws Exception {
    Path inputFile = tempDir.resolve("input.txt");
    Files.writeString(inputFile, "Test Content");

    Path outputFile = tempDir.resolve("output.cce");
    Path tempFile = tempDir.resolve("output.cce.tmp");
    byte[] key = "secret-key".getBytes(StandardCharsets.UTF_8);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        inputFile,
        outputFile,
        tempFile,
        key,
        Files.size(inputFile)
    );

    Executor.execute(context, tracer);

    assertThat(Files.exists(outputFile)).isTrue();
    assertThat(Files.exists(tempFile)).isFalse();

    for (byte b : key) {
      assertThat(b).isEqualTo((byte) 0);
    }
  }

  @Test
  @DisplayName("Edge Case: Null filename fallback to 'root' branch")
  void testExecuteWithRootPath() throws Exception {
    Path rootInput = Path.of("");
    Path outputFile = tempDir.resolve("out.cce");
    Path tempFile = tempDir.resolve("out.cce.tmp");
    byte[] key = "secret".getBytes(StandardCharsets.UTF_8);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        rootInput,
        outputFile,
        tempFile,
        key,
        0L
    );

    assertThatThrownBy(() -> Executor.execute(context, tracer))
        .isInstanceOf(Exception.class);

    for (byte b : key) {
      assertThat(b).isEqualTo((byte) 0);
    }
  }

  @Test
  @DisplayName("Error Path: Processing failure cleans up temp file and sets error")
  void testProcessingFailureCleansUpTempFile() throws IOException {
    Path nonExistentInput = tempDir.resolve("does_not_exist.txt");
    Path outputFile = tempDir.resolve("out.cce");
    Path tempFile = tempDir.resolve("out.cce.tmp");

    Files.createFile(tempFile);
    byte[] key = "secret".getBytes(StandardCharsets.UTF_8);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        nonExistentInput,
        outputFile,
        tempFile,
        key,
        0L
    );

    assertThatThrownBy(() -> Executor.execute(context, tracer))
        .isInstanceOf(Exception.class);

    assertThat(Files.exists(tempFile)).isFalse();
    for (byte b : key) {
      assertThat(b).isEqualTo((byte) 0);
    }
  }

  @Test
  @DisplayName("Branch Coverage: deleteIfExists failure triggers suppressed exception")
  void testTempCleanupFailureSuppression() throws IOException {
    Path inputFile = tempDir.resolve("valid_input.txt");
    Files.writeString(inputFile, "Valid data");

    Path outputFile = tempDir.resolve("out.cce");

    Path tempDirAsFile = tempDir.resolve("mock_temp_dir.tmp");
    Files.createDirectory(tempDirAsFile);
    Files.createFile(tempDirAsFile.resolve("child.txt"));
    byte[] key = "secret".getBytes(StandardCharsets.UTF_8);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        inputFile,
        outputFile,
        tempDirAsFile,
        key,
        Files.size(inputFile)
    );

    assertThatThrownBy(() -> Executor.execute(context, tracer))
        .isInstanceOf(Exception.class)
        .satisfies(ex -> {
          assertThat(ex.getSuppressed()).isNotEmpty();
        });

    for (byte b : key) {
      assertThat(b).isEqualTo((byte) 0);
    }
  }
}