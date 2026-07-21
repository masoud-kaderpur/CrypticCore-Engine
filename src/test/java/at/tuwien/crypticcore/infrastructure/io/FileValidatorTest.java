package at.tuwien.crypticcore.infrastructure.io;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FileValidator Pre-Flight Check Tests")
class FileValidatorTest {

  @TempDir
  Path tempDir;

  private Path validInPath;
  private Path validOutPath;
  private byte[] validKey;

  @BeforeEach
  void setUp() throws IOException {
    validInPath = tempDir.resolve("input.txt");
    validOutPath = tempDir.resolve("output.cce");
    Files.writeString(validInPath, "Valid input content for pre-flight testing.");
    validKey = new byte[] {0x01, 0x02, 0x03, 0x04};
  }

  @Nested
  @DisplayName("Happy Path Input Validation")
  class HappyPathTests {

    @Test
    @DisplayName("Should pass validation when all parameters are valid")
    void shouldPassValidInputs() {
      assertThatCode(() -> FileValidator.isValidInputs(
          CrypticMode.ENCRYPTION,
          validKey,
          validInPath,
          validOutPath,
          Files.size(validInPath)
      )).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("Validation Error Scenarios")
  class FailurePathTests {

    @Test
    @DisplayName("Should throw ValidationException when CrypticMode is null")
    void shouldThrowWhenModeIsNull() {
      assertThatThrownBy(() -> FileValidator.isValidInputs(
          null,
          validKey,
          validInPath,
          validOutPath,
          100
      ))
          .isInstanceOf(ValidationException.class)
          .hasMessage("Mode must not be null");
    }

    @Test
    @DisplayName("Should throw ValidationException when input file does not exist")
    void shouldThrowWhenInputFileDoesNotExist() {
      Path nonExistentInput = tempDir.resolve("non_existent.txt");

      assertThatThrownBy(() -> FileValidator.isValidInputs(
          CrypticMode.ENCRYPTION,
          validKey,
          nonExistentInput,
          validOutPath,
          100
      ))
          .isInstanceOf(ValidationException.class)
          .hasMessage("Input file does not exist: " + nonExistentInput);
    }

    @Test
    @DisplayName("Should throw ValidationException when input and output paths point to the same file")
    void shouldThrowWhenInputAndOutputAreSameFile() {
      assertThatThrownBy(() -> FileValidator.isValidInputs(
          CrypticMode.ENCRYPTION,
          validKey,
          validInPath,
          validInPath,
          100
      ))
          .isInstanceOf(ValidationException.class)
          .hasMessage("Input and output paths must not be the same!");
    }

    @Test
    @DisplayName("Should throw ValidationException when key is null")
    void shouldThrowWhenKeyIsNull() {
      assertThatThrownBy(() -> FileValidator.isValidInputs(
          CrypticMode.ENCRYPTION,
          null,
          validInPath,
          validOutPath,
          100
      ))
          .isInstanceOf(ValidationException.class)
          .hasMessage("Key must not be null or empty");
    }

    @Test
    @DisplayName("Should throw ValidationException when key is empty")
    void shouldThrowWhenKeyIsEmpty() {
      assertThatThrownBy(() -> FileValidator.isValidInputs(
          CrypticMode.ENCRYPTION,
          new byte[0],
          validInPath,
          validOutPath,
          100
      ))
          .isInstanceOf(ValidationException.class)
          .hasMessage("Key must not be null or empty");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -1000L})
    @DisplayName("Should throw ValidationException when file size is 0 or negative")
    void shouldThrowWhenFileSizeIsInvalid(long invalidSize) {
      assertThatThrownBy(() -> FileValidator.isValidInputs(
          CrypticMode.ENCRYPTION,
          validKey,
          validInPath,
          validOutPath,
          invalidSize
      ))
          .isInstanceOf(ValidationException.class)
          .hasMessage("File size must be greater than 0");
    }
  }
}