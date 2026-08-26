//package at.tuwien.crypticcore.infrastructure.io;
//
//import static org.assertj.core.api.Assertions.assertThatCode;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//import at.tuwien.crypticcore.core.domain.Context;
//import at.tuwien.crypticcore.core.domain.exception.ValidationException;
//import at.tuwien.crypticcore.core.domain.model.CrypticMode;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//
//@DisplayName("ContextValidator Pre-Flight Check Tests")
//class ContextValidatorTest {
//
//  @TempDir
//  Path tempDir;
//
//  private Path validInPath;
//  private Path validOutPath;
//  private byte[] validKey;
//  private ContextValidator validator;
//
//  @BeforeEach
//  void setUp() throws IOException {
//    validInPath = tempDir.resolve("input.txt");
//    validOutPath = tempDir.resolve("output.txt");
//    Files.writeString(validInPath, "Valid input content for pre-flight testing.");
//    validKey = new byte[] {0x01, 0x02, 0x03, 0x04};
//    validator = new ContextValidator();
//  }
//
//  @Nested
//  @DisplayName("Happy Path Input Validation")
//  class HappyPathTests {
//
//    @Test
//    @DisplayName("Should pass validation when all parameters are valid")
//    void shouldPassValidInputs() throws IOException {
//      Context context = new Context(CrypticMode.ENCRYPTION, validInPath, validOutPath, validKey, Files.size(validInPath));
//      assertThatCode(() -> validator.validate(context)
//      ).doesNotThrowAnyException();
//    }
//  }
//
//  @Nested
//  @DisplayName("Validation Error Scenarios")
//  class FailurePathTests {
//
//    @Test
//    @DisplayName("Should throw ValidationException when CrypticMode is null")
//    void shouldThrowWhenModeIsNull() {
//      Context context = new Context(null, validInPath, validOutPath, validKey, 100);
//      assertThatThrownBy(() -> validator.validate(context))
//          .isInstanceOf(ValidationException.class)
//          .hasMessage("Mode must not be null");
//    }
//
//    @Test
//    @DisplayName("Should throw ValidationException when input file does not exist")
//    void shouldThrowWhenInputFileDoesNotExist() {
//      Path nonExistentInput = tempDir.resolve("non_existent.txt");
//
//      Context context = new Context(CrypticMode.ENCRYPTION, nonExistentInput, validOutPath, validKey, 100);
//      assertThatThrownBy(() -> validator.validate(context))
//          .isInstanceOf(ValidationException.class)
//          .hasMessage("Input file does not exist: " + nonExistentInput);
//    }
//
//    @Test
//    @DisplayName("Should throw ValidationException when input and output paths point to the same file")
//    void shouldThrowWhenInputAndOutputAreSameFile() {
//      Context context = new Context(CrypticMode.ENCRYPTION, validInPath, validInPath, validKey, 100);
//
//      assertThatThrownBy(() -> validator.validate(context))
//          .isInstanceOf(ValidationException.class)
//          .hasMessage("Input and output paths must not be the same!");
//    }
//
//    @Test
//    @DisplayName("Should throw ValidationException when key is null")
//    void shouldThrowWhenKeyIsNull() {
//      Context context = new Context(CrypticMode.ENCRYPTION, validInPath, validOutPath, null, 100);
//
//      assertThatThrownBy(() -> validator.validate(context))
//          .isInstanceOf(ValidationException.class)
//          .hasMessage("Key must not be null or empty");
//    }
//
//    @Test
//    @DisplayName("Should throw ValidationException when key is empty")
//    void shouldThrowWhenKeyIsEmpty() {
//      Context context = new Context(CrypticMode.ENCRYPTION, validInPath, validOutPath, new byte[0], 100);
//
//      assertThatThrownBy(() -> validator.validate(context))
//          .isInstanceOf(ValidationException.class)
//          .hasMessage("Key must not be null or empty");
//    }
//
//    @ParameterizedTest
//    @ValueSource(longs = {0L, -1L, -1000L})
//    @DisplayName("Should throw ValidationException when file size is 0 or negative")
//    void shouldThrowWhenFileSizeIsInvalid(long invalidSize) {
//      Context context = new Context(CrypticMode.ENCRYPTION, validInPath, validOutPath, validKey, invalidSize);
//
//      assertThatThrownBy(() -> validator.validate(context))
//          .isInstanceOf(ValidationException.class)
//          .hasMessage("File size must be greater than 0");
//    }
//  }
//}