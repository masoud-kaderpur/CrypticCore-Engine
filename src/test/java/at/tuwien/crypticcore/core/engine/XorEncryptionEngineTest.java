package at.tuwien.crypticcore.core.engine;

import static at.tuwien.crypticcore.core.domain.FormatSpecification.getHeaderLength;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.Validator;
import at.tuwien.crypticcore.core.domain.exception.DataTruncationException;
import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import at.tuwien.crypticcore.infrastructure.io.ContextValidator;
import at.tuwien.crypticcore.infrastructure.io.HeaderHandler;
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

class XorEncryptionEngineTest {

  @TempDir
  Path tempDir;

  private CipherAlgorithm algorithm;
  private Validator validator;
  private HeaderCodec headerCodec;
  private Tracer tracer;
  private XorEncryptionEngine engine;

  @BeforeEach
  void setUp() {
    algorithm = new XorCipher();
    validator = new ContextValidator();
    headerCodec = new HeaderHandler();
    tracer = OpenTelemetry.noop().getTracer("test-tracer");
    engine = new XorEncryptionEngine(algorithm, validator, headerCodec, tracer);
  }

  @Test
  @DisplayName("Happy Path: Full round-trip encryption and decryption with multi-chunk buffer payload")
  void testEncryptionAndDecryptionMultiChunk() throws IOException {
    byte[] rawData = new byte[10_000];
    for (int i = 0; i < rawData.length; i++) {
      rawData[i] = (byte) (i % 127);
    }

    Path plainFile = tempDir.resolve("plain.txt");
    Path cipherTemp = tempDir.resolve("cipher.tmp");
    Path decryptedTemp = tempDir.resolve("decrypted.tmp");
    byte[] key = "super-secret-key".getBytes(StandardCharsets.UTF_8);

    Files.write(plainFile, rawData);

    Context encContext = new Context(
        CrypticMode.ENCRYPTION,
        plainFile,
        tempDir.resolve("cipher.cce"),
        cipherTemp,
        key.clone(),
        Files.size(plainFile)
    );
    engine.process(encContext);

    assertThat(Files.exists(cipherTemp)).isTrue();
    assertThat(Files.size(cipherTemp)).isEqualTo(rawData.length + getHeaderLength());

    Context decContext = new Context(
        CrypticMode.DECRYPTION,
        cipherTemp,
        tempDir.resolve("decrypted.txt"),
        decryptedTemp,
        key.clone(),
        Files.size(cipherTemp)
    );
    engine.process(decContext);

    assertThat(Files.exists(decryptedTemp)).isTrue();
    assertThat(Files.readAllBytes(decryptedTemp)).isEqualTo(rawData);
  }

  @Test
  @DisplayName("Branch: Null filename path exercises safe fallback to 'root'")
  void testNullFileNameFallback() {
    Path rootPath = Path.of("");
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        rootPath,
        tempDir.resolve("out.cce"),
        tempDir.resolve("out.cce.tmp"),
        key,
        100L
    );

    assertThatThrownBy(() -> engine.process(context))
        .isInstanceOf(IOException.class);
  }

  @Test
  @DisplayName("Branch: Data truncation check fails during ENCRYPTION mode")
  void testDataTruncationEncryption() throws IOException {
    Path plainFile = tempDir.resolve("trunc_input.txt");
    Files.writeString(plainFile, "Short content");

    Path tempOutput = tempDir.resolve("trunc_out.tmp");
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);

    long actualSize = Files.size(plainFile);
    long inflatedSize = actualSize + 50L;

    Validator bypassValidator = ctx -> {};
    XorEncryptionEngine customEngine = new XorEncryptionEngine(algorithm, bypassValidator, headerCodec, tracer);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        plainFile,
        tempDir.resolve("out.cce"),
        tempOutput,
        key,
        inflatedSize
    );

    assertThatThrownBy(() -> customEngine.process(context))
        .isInstanceOf(DataTruncationException.class)
        .hasMessageContaining("data truncation during encryption!");
  }

  @Test
  @DisplayName("Branch: Data truncation check fails during DECRYPTION mode")
  void testDataTruncationDecryption() throws IOException {
    Path plainFile = tempDir.resolve("plain_for_dec.txt");
    Files.writeString(plainFile, "Payload");
    Path cipherTemp = tempDir.resolve("cipher_valid.tmp");
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);

    Context encContext = new Context(
        CrypticMode.ENCRYPTION,
        plainFile,
        tempDir.resolve("out.cce"),
        cipherTemp,
        key.clone(),
        Files.size(plainFile)
    );
    engine.process(encContext);

    long actualCipherSize = Files.size(cipherTemp);
    long corruptSize = actualCipherSize + 10L;

    Validator bypassValidator = ctx -> {};
    XorEncryptionEngine customEngine = new XorEncryptionEngine(algorithm, bypassValidator, headerCodec, tracer);

    Context decContext = new Context(
        CrypticMode.DECRYPTION,
        cipherTemp,
        tempDir.resolve("final_dec.txt"),
        tempDir.resolve("final_dec.tmp"),
        key.clone(),
        corruptSize
    );

    assertThatThrownBy(() -> customEngine.process(decContext))
        .isInstanceOf(DataTruncationException.class)
        .hasMessageContaining("data truncation during decryption!");
  }

  @Test
  @DisplayName("Branch: Catches generic RuntimeException not derived from CrypticException")
  void testCatchGenericRuntimeException() throws IOException {
    Path inputFile = tempDir.resolve("runtime_input.txt");
    Files.writeString(inputFile, "Test");
    Path tempOut = tempDir.resolve("runtime_out.tmp");
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);

    Validator faultyValidator = ctx -> {
      throw new IllegalStateException("simulated unchecked runtime failure");
    };

    XorEncryptionEngine faultyEngine = new XorEncryptionEngine(algorithm, faultyValidator, headerCodec, tracer);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        inputFile,
        tempDir.resolve("out.cce"),
        tempOut,
        key,
        Files.size(inputFile)
    );

    assertThatThrownBy(() -> faultyEngine.process(context))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("simulated unchecked runtime failure");
  }

  @Test
  @DisplayName("Branch: Catches IOException when input stream target is missing")
  void testCatchIOException() {
    Path missingInput = tempDir.resolve("missing_on_filesystem.txt");
    Path tempOut = tempDir.resolve("io_out.tmp");
    byte[] key = "key".getBytes(StandardCharsets.UTF_8);

    Validator bypassValidator = ctx -> {};
    XorEncryptionEngine ioEngine = new XorEncryptionEngine(algorithm, bypassValidator, headerCodec, tracer);

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        missingInput,
        tempDir.resolve("out.cce"),
        tempOut,
        key,
        100L
    );

    assertThatThrownBy(() -> ioEngine.process(context))
        .isInstanceOf(IOException.class);
  }
}