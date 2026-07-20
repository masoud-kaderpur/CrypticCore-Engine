package at.tuwien.crypticcore.core.engine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.infrastructure.io.ProgressObserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XorStreamProcessorTest {

  private XorProcessor processor;
  private List<Integer> progressUpdates;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    XorCipher cipher = new XorCipher();
    progressUpdates = new ArrayList<>();
    ProgressObserver observer = percentage -> progressUpdates.add(percentage);
    processor = new XorProcessor(cipher, observer);
  }

  @Test
  void testEncryptionAndDecryption_SuccessfulLifecycle() throws IOException {
    // Prepare pure plain text mock file
    Path plainFile = tempDir.resolve("input.txt");
    Path encryptedFile = tempDir.resolve("output.cce");
    Path decryptedFile = tempDir.resolve("restored.txt");

    byte[] originalData = "Hello Clean Architecture SOLID Rules!".getBytes();
    Files.write(plainFile, originalData);
    byte[] key = "SecretKey".getBytes();

    // 1. Run Encryption Loop
    processor.processFile(CrypticMode.ENCRYPTION, plainFile.toString(), encryptedFile.toString(), key, originalData.length);

    assertTrue(Files.exists(encryptedFile), "Encrypted file must be physically written to disk");
    assertFalse(progressUpdates.isEmpty(),
        "Progress observer must receive updates during runtime loops");
    assertEquals(100, progressUpdates.getLast(), "The terminal progress percentage must hit 100%");

    // 2. Run Decryption Loop (using the calculated expected payload size: total minus 4 header bytes)
    long cipherFileSize = Files.size(encryptedFile) - 4;
    progressUpdates.clear(); // clear for decryption verification

    processor.processFile(CrypticMode.DECRYPTION, encryptedFile.toString(), decryptedFile.toString(), key, cipherFileSize);

    // 3. Verify Integrity Assertions
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

    // Intentionally pass an incorrect, larger expected file size parameter to simulate stream truncation mismatch
    long mismatchedSize = originalData.length + 500;

    IOException exception = assertThrows(IOException.class, () -> {
      processor.processFile(CrypticMode.ENCRYPTION, plainFile.toString(), encryptedFile.toString(), key, mismatchedSize);
    });

    assertTrue(exception.getMessage().contains("Data truncation detected!"), "Should crash cleanly with clear data truncation messages");
  }
}