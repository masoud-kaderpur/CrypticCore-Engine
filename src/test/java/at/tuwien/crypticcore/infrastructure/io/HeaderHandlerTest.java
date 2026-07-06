package at.tuwien.crypticcore.infrastructure.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HeaderHandlerTest {

  @TempDir
  Path tempDir;

  @Test
  void testHeaderLifecycle_ShouldWriteAndVerifyValidHeader() throws IOException {
    Path testFile = tempDir.resolve("header_lifecycle.dat");

    // 1. Write the header to a real temporary file on disk
    try (FileOutputStream out = new FileOutputStream(testFile.toFile())) {
      HeaderHandler.writeHeader(out);
    }

    // Verify file length criteria has header content written
    long fileLength = Files.size(testFile);
    assertTrue(fileLength >= 4, "A standard file header must contain at least a 4-byte sequence");

    // 2. Read and verify using the valid file input stream
    try (FileInputStream in = new FileInputStream(testFile.toFile())) {
      assertDoesNotThrow(() -> HeaderHandler.checkHeader(in),
          "Valid headers must pass structural verification loops smoothly");
    }
  }

  @Test
  void checkHeader_ShouldThrowIOException_WhenHeaderIsTruncated() throws IOException {
    Path shortFile = tempDir.resolve("truncated.dat");
    // Write incomplete data (only 2 bytes instead of 4)
    byte[] truncatedBytes = {0x43, 0x43};
    Files.write(shortFile, truncatedBytes);

    try (FileInputStream in = new FileInputStream(shortFile.toFile())) {
      assertThrows(IOException.class, () -> HeaderHandler.checkHeader(in));
    }
  }

  @Test
  void checkHeader_ShouldThrowIOException_WhenMagicBytesAreInvalid() throws IOException {
    Path badFile = tempDir.resolve("corrupt.dat");
    // Write 4 complete bytes but with completely incorrect magic signature data
    byte[] garbageBytes = {0x42, 0x41, 0x44, 0x21};
    Files.write(badFile, garbageBytes);

    try (FileInputStream in = new FileInputStream(badFile.toFile())) {
      assertThrows(IOException.class, () -> HeaderHandler.checkHeader(in));
    }
  }
}