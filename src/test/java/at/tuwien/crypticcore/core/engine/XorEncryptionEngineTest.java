package at.tuwien.crypticcore.core.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XorEncryptionEngineTest {

  private XorEncryptionEngine processor;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    XorCipher cipher = new XorCipher();
    processor = new XorEncryptionEngine(cipher);
  }

  @Test
  void testEncryptionAndDecryption_SuccessfulLifecycle() throws IOException {
    Path plainFile = tempDir.resolve("input.txt");
    Path encryptedFile = tempDir.resolve("output.cce");
    Path decryptedFile = tempDir.resolve("restored.txt");

    byte[] originalData = "Hello Clean Architecture SOLID Rules!".getBytes();
    Files.write(plainFile, originalData);
    byte[] key = "SecretKey".getBytes();

    processor.processFile(CrypticMode.ENCRYPTION, plainFile.toString(), encryptedFile.toString(), key, originalData.length);

    assertTrue(Files.exists(encryptedFile), "Encrypted file must be physically written to disk");

    long encryptedFileSize = Files.size(encryptedFile);

    processor.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, encryptedFileSize);

    byte[] restoredData = Files.readAllBytes(decryptedFile);
    assertArrayEquals(originalData, restoredData, "Decrypted byte array content must match original text perfectly");
  }

  @Test
  void testProcessFile_ShouldThrowIOException_WhenDataTruncationDetected() throws IOException {
    Path plainFile = tempDir.resolve("trunc_input.txt");
    Path encryptedFile = tempDir.resolve("trunc_output.cce");

    byte[] originalData = "Truncation Verification Block".getBytes();
    Files.write(plainFile, originalData);
    byte[] key = "Key".getBytes();

    long mismatchedSize = originalData.length + 500;

    IOException exception = assertThrows(IOException.class, () -> {
      processor.processFile(CrypticMode.ENCRYPTION, plainFile.toString(), encryptedFile.toString(), key, mismatchedSize);
    });

    assertTrue(exception.getMessage().contains("Data truncation during encryption"),
        "Should crash cleanly with clear data truncation messages");
  }
}