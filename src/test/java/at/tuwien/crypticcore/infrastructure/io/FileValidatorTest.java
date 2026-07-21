//package at.tuwien.crypticcore.infrastructure.io;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import at.tuwien.crypticcore.core.domain.model.CrypticMode;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//
//class FileValidatorTest {
//
//  @TempDir
//  Path tempDir;
//
//  private Path existingInput;
//  private Path targetOutput;
//  private byte[] standardKey;
//
//  @BeforeEach
//  void setUp() throws IOException {
//    existingInput = tempDir.resolve("real_source.txt");
//    targetOutput = tempDir.resolve("target_output.cce");
//    Files.write(existingInput, "Validating IO layers stream data".getBytes());
//    standardKey = "SecureKey123".getBytes();
//  }
//
//  @Test
//  void isValidInputs_ShouldPass_WithValidParameters() {
//    assertDoesNotThrow(() -> {
//      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, standardKey, existingInput, targetOutput, 10L);
//    });
//  }
//
//  @Test
//  void isValidInputs_ShouldThrowException_WhenModeIsNull() {
//    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
//      FileValidator.isValidInputs(null, standardKey, existingInput, targetOutput, 10L);
//    });
//    assertTrue(ex.getMessage().contains("Mode must not be null"));
//  }
//
//  @Test
//  void isValidInputs_ShouldThrowFileNotFoundException_WhenInputFileDoesNotExist() {
//    Path missingInput = tempDir.resolve("non_existent_ghost_file.txt");
//
//    assertThrows(FileNotFoundException.class, () -> {
//      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, standardKey, missingInput, targetOutput, 10L);
//    });
//  }
//
//  @Test
//  void isValidInputs_ShouldThrowException_WhenPathsAreIdentical() throws IOException {
//    // To trigger the branch, outPath must physically exist on disk
//    Path identicalOutput = tempDir.resolve("identical_target.txt");
//    Files.write(identicalOutput, "Existing data".getBytes());
//
//    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
//      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, standardKey, identicalOutput, identicalOutput, 10L);
//    });
//    assertTrue(ex.getMessage().contains("Input and output paths must not be the same"));
//  }
//
//  @Test
//  void isValidInputs_ShouldThrowException_WhenKeyIsEmptyOrNull() {
//    IllegalArgumentException nullKeyEx = assertThrows(IllegalArgumentException.class, () -> {
//      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, null, existingInput, targetOutput, 10L);
//    });
//    assertTrue(nullKeyEx.getMessage().contains("Key must not be null or empty"));
//
//    IllegalArgumentException emptyKeyEx = assertThrows(IllegalArgumentException.class, () -> {
//      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, new byte[0], existingInput, targetOutput, 10L);
//    });
//    assertTrue(emptyKeyEx.getMessage().contains("Key must not be null or empty"));
//  }
//
//  @Test
//  void isValidInputs_ShouldThrowException_WhenFileSizeIsZero() {
//    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
//      FileValidator.isValidInputs(CrypticMode.ENCRYPTION, standardKey, existingInput, targetOutput, 0L);
//    });
//    assertTrue(ex.getMessage().contains("File size must be >0"));
//  }
//}