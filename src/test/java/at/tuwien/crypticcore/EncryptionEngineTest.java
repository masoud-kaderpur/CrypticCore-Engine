package at.tuwien.crypticcore;

import at.tuwien.crypticcore.engine.CrypticMode;
import at.tuwien.crypticcore.engine.EncryptionEngine;
import at.tuwien.crypticcore.engine.XorCipher;
import at.tuwien.crypticcore.io.ProgressObserver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Integration test suite for the {@link EncryptionEngine}.
 * <p>Verifies the interplay between stream-based processing, the file system,
 * and cryptographic strategies. These tests enforce the "CCE" file format
 * specification and atomic integrity constraints.</p>
 */
public class EncryptionEngineTest {

  @TempDir
  Path tempDir;

  private final byte[] key = "secret".getBytes(StandardCharsets.UTF_8);
  private final ProgressObserver observer = percentage -> {
  };
  private final EncryptionEngine engine = new EncryptionEngine(new XorCipher(), observer, new SimpleMeterRegistry());
  private Path inputFile;
  private Path encryptedFile;
  private Path decryptedFile;

  @BeforeEach
  void setUp() throws IOException {
    inputFile = tempDir.resolve("input.bin");
    encryptedFile = tempDir.resolve("encrypted.cce");
    decryptedFile = tempDir.resolve("decrypted.bin");
    Files.write(inputFile, new byte[]{0, -1, 127, -128, 65, 66, 67});
  }

  /**
   * Verifies a complete encryption/decryption lifecycle.
   * <p>Checks that:
   * 1. The custom CCE v1 header is correctly prepended during encryption.
   * 2. The transformation is reversible via the {@link XorCipher}.
   * 3. The resulting file matches the original byte-for-byte.</p>
   */
  @Test
  @DisplayName("Integration: End-to-End Cycle")
  void testFullCycle() throws IOException {
    long inputSize = Files.size(inputFile);
    engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), encryptedFile.toString(), key, inputSize);

    byte[] encryptedBytes = Files.readAllBytes(encryptedFile);
    byte[] expectedHeader = new byte[]{'C', 'C', 'E', 1};
    for (int i = 0; i < expectedHeader.length; i++) {
      Assertions.assertEquals(expectedHeader[i], encryptedBytes[i], "Header byte mismatch at " + i);
    }

    long encryptedSize = Files.size(encryptedFile);
    engine.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, encryptedSize - 4);

    byte[] decryptedBytes = Files.readAllBytes(decryptedFile);
    byte[] originalBytes = Files.readAllBytes(inputFile);
    Assertions.assertArrayEquals(originalBytes, decryptedBytes, "Decrypted data must match original");
  }

  @Test
  @DisplayName("Error Resilience: Truncated File")
  void testTruncation() {
    Assertions.assertThrows(IOException.class, () -> {
      long inputSize = Files.size(inputFile);
      engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), encryptedFile.toString(), key, inputSize + 1);
    });
  }

  @Test
  @DisplayName("Error Resilience: Same File Validation")
  void testSameFile() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      long inputSize = Files.size(inputFile);
      engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(), inputFile.toString(), key, inputSize);
    });
  }

  @Test
  @DisplayName("Integration: Header Validation")
  void testInvalidHeader() throws IOException {
    byte[] badFile = new byte[]{'B', 'A', 'D', 1, 42};
    Files.write(encryptedFile, badFile);
    long sizeForProgress = Files.size(encryptedFile) - 4;
    Assertions.assertThrows(IOException.class, () -> {
      engine.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, sizeForProgress);
    });

  }

  @Test
  @DisplayName("Integration: Header Validation V2")
  void testInvalidHeaderV2() throws IOException {
    byte[] badFile = new byte[]{'A', 'A'};
    Files.write(encryptedFile, badFile);
    long sizeForProgress = Files.size(encryptedFile) - 4;
    Assertions.assertThrows(IOException.class, () -> {
      engine.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, sizeForProgress);
    });
  }

  @Test
  @DisplayName("Integration: Version Validation")
  void testInvalidVersion() throws IOException {
    byte[] badVersionFile = new byte[]{'C', 'C', 'E', 2, 42};
    Files.write(encryptedFile, badVersionFile);
    long sizeForProgress = Files.size(encryptedFile) - 4;
    Assertions.assertThrows(IOException.class, () -> {
      engine.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, sizeForProgress);
    });
  }

  @Test
  @DisplayName("Integration: CryptionMode Validation")
  void testCryptionMode() throws IOException {
    byte[] badVersionFile = new byte[]{'C', 'C', 'E', 2, 42};
    Files.write(encryptedFile, badVersionFile);
    long sizeForProgress = Files.size(encryptedFile) - 4;
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      engine.processFile(null, encryptedFile.toString(), decryptedFile.toString(), key,
              sizeForProgress);
    });
  }

  @Test
  @DisplayName("Integration:  File Validation")
  void testFile() throws IOException {
    Assertions.assertThrows(FileNotFoundException.class, () -> {
      engine.processFile(CrypticMode.ENCRYPTION, " ",
              decryptedFile.toString(), key, 1
      );
    });
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      engine.processFile(CrypticMode.ENCRYPTION, inputFile.toString(),
              decryptedFile.toString(), key, 0
      );
    });
  }
}

