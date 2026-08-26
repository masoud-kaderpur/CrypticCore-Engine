package at.tuwien.crypticcore.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.Validator;
import at.tuwien.crypticcore.core.domain.exception.DataTruncationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import at.tuwien.crypticcore.infrastructure.io.ContextValidator;
import at.tuwien.crypticcore.infrastructure.io.HeaderHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("XorEncryptionEngine Stream Integration & Boundary Tests")
class XorEncryptionEngineTest {

  @TempDir
  Path tempDir;

  private XorEncryptionEngine engine;

  @BeforeEach
  void setUp() {
    CipherAlgorithm cipher = new XorCipher();
    Validator validator = new ContextValidator();
    HeaderCodec headerCodec = new HeaderHandler();
    this.engine = new XorEncryptionEngine(cipher, validator, headerCodec);
  }

  @Nested
  @DisplayName("Happy Path File Transformations")
  class HappyPathTests {

    @Test
    @DisplayName("Should successfully encrypt a file writing a 4-byte header + payload")
    void shouldEncryptFileSuccessfully() throws Exception {
      Path inputPath = tempDir.resolve("plain.txt");
      Path outputPath = tempDir.resolve("encrypted.cce");
      String content = "High Performance OpenTelemetry Encrypted Payload 2026!";
      byte[] input = content.getBytes(StandardCharsets.UTF_8);
      byte[] key = "SecretKey123".getBytes(StandardCharsets.UTF_8);
      long size = input.length;

      Context context = new Context(CrypticMode.ENCRYPTION, inputPath, outputPath, key, size);

      Files.write(inputPath, input);

      engine.process(context);

      assertThat(outputPath).exists();
      assertThat(Files.size(outputPath)).isEqualTo(size + 4);
      assertThat(Files.readAllBytes(outputPath)).isNotEqualTo(input);
    }

    @Test
    @DisplayName("Should successfully perform complete Encryption -> Decryption cycle")
    void shouldEncryptAndDecryptCycle() throws Exception {
      Path inputPath = tempDir.resolve("original.txt");
      Path encryptedPath = tempDir.resolve("transformed.txt");
      Path decryptedPath = tempDir.resolve("restored.txt");

      String secretText = "Testing streaming byte-by-byte integrity across 8KB buffer boundaries.";
      byte[] key = "DynatraceKey2026".getBytes(StandardCharsets.UTF_8);

      Files.writeString(inputPath, secretText, StandardCharsets.UTF_8);
      long originalSize = Files.size(inputPath);

      Context context = new Context(CrypticMode.ENCRYPTION, inputPath, encryptedPath, key, originalSize);

      engine.process(context);

      long encryptedSize = Files.size(encryptedPath);

      context =  new Context(CrypticMode.DECRYPTION, encryptedPath, decryptedPath, key, encryptedSize);

      engine.process(context);

      assertThat(decryptedPath).exists();
      assertThat(Files.readString(decryptedPath, StandardCharsets.UTF_8)).isEqualTo(secretText);
    }
  }

  @Nested
  @DisplayName("Data Truncation & Failure Scenarios")
  class DataTruncationAndFailureTests {

    @Test
    @DisplayName("Should throw DataTruncationException when reported size differs during encryption")
    void shouldThrowDataTruncationOnEncryptionSizeMismatch() throws IOException {
      Path inputPath = tempDir.resolve("data.txt");
      Path outputPath = tempDir.resolve("data.cce");
      byte[] key = "Key".getBytes(StandardCharsets.UTF_8);

      Files.writeString(inputPath, "1234567890");

      long size = 20;

      Context context = new Context(CrypticMode.ENCRYPTION, inputPath, outputPath, key, size);
      assertThatThrownBy(() ->
          engine.process(context)
      )
          .isInstanceOf(DataTruncationException.class)
          .hasMessageContaining("Data truncation during encryption!");
    }

    @Test
    @DisplayName("Should throw DataTruncationException when reported size differs during decryption")
    void shouldThrowDataTruncationOnDecryptionSizeMismatch() throws Exception {
      Path inputPath = tempDir.resolve("plain.txt");
      Path encryptedPath = tempDir.resolve("encrypted.cce");
      Path decryptedPath = tempDir.resolve("restored.txt");
      byte[] key = "Key".getBytes(StandardCharsets.UTF_8);

      String content = "Test Content";
      Files.writeString(inputPath, content, StandardCharsets.UTF_8);
      long originalSize = Files.size(inputPath);

      Context context = new Context(CrypticMode.ENCRYPTION, inputPath, encryptedPath, key, originalSize);
      engine.process(context);

      long wrongEncryptedSize = 50;

      Context context2 = new Context(CrypticMode.DECRYPTION, encryptedPath, decryptedPath, key, wrongEncryptedSize);

      assertThatThrownBy(() ->
          engine.process(context2)
      )
          .isInstanceOf(DataTruncationException.class)
          .hasMessageContaining("Data truncation during decryption!");
    }
  }
}