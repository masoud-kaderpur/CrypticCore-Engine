package at.tuwien.crypticcore.infrastructure.io;

import static at.tuwien.crypticcore.core.domain.FormatSpecification.getHeaderLength;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContextValidatorTest {

  @TempDir
  Path tempDir;

  private ContextValidator validator;
  private Path existingInput;
  private Path nonExistingOutput;
  private Path tempOutput;
  private byte[] validKey;

  @BeforeEach
  void setUp() throws IOException {
    validator = new ContextValidator();
    existingInput = tempDir.resolve("input.txt");
    Files.writeString(existingInput, "standard file content");

    nonExistingOutput = tempDir.resolve("output.cce");
    tempOutput = tempDir.resolve("output.cce.tmp");
    validKey = "secret-key".getBytes(StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Constructor invocation for 100% class line coverage")
  void testConstructor() {
    assertThat(new ContextValidator()).isNotNull();
  }

  @Test
  @DisplayName("Happy Path: Valid encryption context passes validation")
  void testValidateHappyPathEncryption() {
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        100L
    );

    assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Happy Path: Valid decryption context with fileSize >= header passes validation")
  void testValidateHappyPathDecryption() {
    Context context = new Context(
        CrypticMode.DECRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        (long) getHeaderLength() + 10L
    );

    assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Branch: mode == null throws ValidationException")
  void testModeNull() {
    Context context = new Context(
        null,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        100L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessage("mode must not be null");
  }

  @Test
  @DisplayName("Branch: !Files.exists(inputPath) throws ValidationException")
  void testInputFileDoesNotExist() {
    Path missingInput = tempDir.resolve("missing.txt");
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        missingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        100L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessageStartingWith("input path does not exist:");
  }

  @Test
  @DisplayName("Branch: Output file exists but is NOT same file passes")
  void testOutputFileExistsDifferentFile() throws IOException {
    Path existingOutput = tempDir.resolve("existing_output.cce");
    Files.writeString(existingOutput, "pre-existing content");

    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        existingOutput,
        tempOutput,
        validKey,
        100L
    );

    assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Branch: Output file exists AND is same file throws ValidationException")
  void testInputAndOutputAreSameFile() {
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        existingInput,
        tempOutput,
        validKey,
        100L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessage("input and output paths must not be the same!");
  }

  @Test
  @DisplayName("Branch: key == null throws ValidationException")
  void testKeyNull() {
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        null,
        100L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Key must not be null or empty");
  }

  @Test
  @DisplayName("Branch: key.length == 0 throws ValidationException")
  void testKeyEmpty() {
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        new byte[0],
        100L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Key must not be null or empty");
  }

  @Test
  @DisplayName("Branch: fileSize == 0 throws ValidationException")
  void testFileSizeZero() {
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        0L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessage("File size must be greater than 0");
  }

  @Test
  @DisplayName("Branch: fileSize < 0 throws ValidationException")
  void testFileSizeNegative() {
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        -5L
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessage("File size must be greater than 0");
  }

  @Test
  @DisplayName("Branch: DECRYPTION mode with fileSize < headerLength throws ValidationException")
  void testDecryptionFileSizeSmallerThanHeader() {
    long invalidSmallSize = (long) getHeaderLength() - 1L;
    Context context = new Context(
        CrypticMode.DECRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        invalidSmallSize
    );

    assertThatThrownBy(() -> validator.validate(context))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("is smaller than the required header");
  }

  @Test
  @DisplayName("Branch: ENCRYPTION mode with fileSize < headerLength is ALLOWED")
  void testEncryptionFileSizeSmallerThanHeaderAllowed() {
    long smallInputSize = (long) getHeaderLength() - 1L;
    Context context = new Context(
        CrypticMode.ENCRYPTION,
        existingInput,
        nonExistingOutput,
        tempOutput,
        validKey,
        smallInputSize
    );

    assertThatCode(() -> validator.validate(context)).doesNotThrowAnyException();
  }
}