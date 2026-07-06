package at.tuwien.crypticcore.infrastructure.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.tuwien.crypticcore.core.domain.CrypticMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileValidatorTest {

  @TempDir
  Path tempDir;

  @Test
  void isValidInputs_ShouldPass_WithValidParameters() throws IOException {
    Path input = tempDir.resolve("source.txt");
    Path output = tempDir.resolve("target.cce");

    byte[] mockContent = "Validating IO layers step by step".getBytes();
    Files.write(input, mockContent);
    byte[] key = "SecureKey".getBytes();

    assertDoesNotThrow(() -> {
      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, key, input, output, mockContent.length);
    });
  }

  @Test
  void isValidInputs_ShouldThrowException_WhenKeyIsEmptyOrNull() {
    Path input = tempDir.resolve("any.txt");
    Path output = tempDir.resolve("any.cce");

    IllegalArgumentException nullKeyEx = assertThrows(IllegalArgumentException.class, () -> {
      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, null, input, output, 10L);
    });
    assertTrue(nullKeyEx.getMessage().contains("Key cannot be null or empty"));

    IllegalArgumentException emptyKeyEx = assertThrows(IllegalArgumentException.class, () -> {
      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, new byte[0], input, output, 10L);
    });
    assertTrue(emptyKeyEx.getMessage().contains("Key cannot be null or empty"));
  }

  @Test
  void isValidInputs_ShouldThrowException_WhenPathsAreIdentical() {
    Path input = tempDir.resolve("identical.txt");
    byte[] key = {1, 2, 3};

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, key, input, input, 10L);
    });
    assertTrue(ex.getMessage().contains("Input and output paths cannot be identical"));
  }

  @Test
  void isValidInputs_ShouldThrowException_WhenInputFileDoesNotExist() {
    Path missingInput = tempDir.resolve("ghost_file.txt");
    Path output = tempDir.resolve("target.cce");
    byte[] key = {1, 2, 3};

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, key, missingInput, output, 10L);
    });
    assertTrue(ex.getMessage().contains("Input file does not exist"));
  }

  @Test
  void isValidInputs_ShouldThrowException_WhenFileSizeIsNegative() throws IOException {
    Path input = tempDir.resolve("source.txt");
    Path output = tempDir.resolve("target.cce");
    Files.write(input, "Data".getBytes());
    byte[] key = {1, 2, 3};

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, key, input, output, -1L);
    });
    assertTrue(ex.getMessage().contains("Invalid file size"));
  }
}