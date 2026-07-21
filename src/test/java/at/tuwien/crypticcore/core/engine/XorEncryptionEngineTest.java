package at.tuwien.crypticcore.core.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.exception.DataTruncationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
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
  private CipherAlgorithm cipher;

  @BeforeEach
  void setUp() {
    this.cipher = new XorCipher();
    this.engine = new XorEncryptionEngine(cipher);
  }

  @Nested
  @DisplayName("Happy Path File Transformations")
  class HappyPathTests {

    @Test
    @DisplayName("Should successfully encrypt a file writing a 4-byte header + payload")
    void shouldEncryptFileSuccessfully() throws Exception {
      Path inputFile = tempDir.resolve("plain.txt");
      Path outputFile = tempDir.resolve("encrypted.cce");
      String content = "High Performance OpenTelemetry Encrypted Payload 2026!";
      byte[] inputBytes = content.getBytes(StandardCharsets.UTF_8);
      byte[] key = "SecretKey123".getBytes(StandardCharsets.UTF_8);

      Files.write(inputFile, inputBytes);
      long fileSize = inputBytes.length;

      engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), outputFile.toString(), key, fileSize);

      assertThat(outputFile).exists();
      assertThat(Files.size(outputFile)).isEqualTo(fileSize + 4);
      assertThat(Files.readAllBytes(outputFile)).isNotEqualTo(inputBytes);
    }

    @Test
    @DisplayName("Should successfully perform complete Encryption -> Decryption cycle")
    void shouldEncryptAndDecryptCycle() throws Exception {
      Path inputFile = tempDir.resolve("original.txt");
      Path encryptedFile = tempDir.resolve("transformed.cce");
      Path decryptedFile = tempDir.resolve("restored.txt");

      String secretText = "Testing streaming byte-by-byte integrity across 8KB buffer boundaries.";
      byte[] key = "DynatraceKey2026".getBytes(StandardCharsets.UTF_8);

      Files.writeString(inputFile, secretText, StandardCharsets.UTF_8);
      long originalSize = Files.size(inputFile);

      engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), encryptedFile.toString(), key, originalSize);

      long encryptedSize = Files.size(encryptedFile);
      engine.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, encryptedSize);

      assertThat(decryptedFile).exists();
      assertThat(Files.readString(decryptedFile, StandardCharsets.UTF_8)).isEqualTo(secretText);
    }
  }

  @Nested
  @DisplayName("Data Truncation & Failure Scenarios")
  class DataTruncationAndFailureTests {

    @Test
    @DisplayName("Should throw DataTruncationException when reported size differs during encryption")
    void shouldThrowDataTruncationOnEncryptionSizeMismatch() throws IOException {
      Path inputFile = tempDir.resolve("data.txt");
      Path outputFile = tempDir.resolve("data.cce");
      byte[] key = "Key".getBytes(StandardCharsets.UTF_8);

      byte[] actualBytes = "1234567890".getBytes(StandardCharsets.UTF_8);
      Files.write(inputFile, actualBytes);

      long fakeReportedSize = 20;

      assertThatThrownBy(() ->
          engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), outputFile.toString(), key, fakeReportedSize)
      )
          .isInstanceOf(DataTruncationException.class)
          .hasMessageContaining("Data truncation during encryption!");
    }

    @Test
    @DisplayName("Should throw DataTruncationException when reported size differs during decryption")
    void shouldThrowDataTruncationOnDecryptionSizeMismatch() throws Exception {
      Path inputFile = tempDir.resolve("plain.txt");
      Path encryptedFile = tempDir.resolve("encrypted.cce");
      Path decryptedFile = tempDir.resolve("restored.txt");
      byte[] key = "Key".getBytes(StandardCharsets.UTF_8);

      String content = "Test Content";
      Files.writeString(inputFile, content, StandardCharsets.UTF_8);
      long originalSize = Files.size(inputFile);

      engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), encryptedFile.toString(), key, originalSize);

      long wrongEncryptedSize = 50;

      assertThatThrownBy(() ->
          engine.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, wrongEncryptedSize)
      )
          .isInstanceOf(DataTruncationException.class)
          .hasMessageContaining("Data truncation during decryption!");
    }
  }
}