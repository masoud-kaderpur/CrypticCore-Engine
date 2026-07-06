package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.CrypticMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link CrypticMode} enumeration.
 * Ensures that string-to-mode conversion is case-insensitive and robust
 * against invalid inputs.
 */
public class CrypticModeTest {

  @ParameterizedTest
  @CsvSource({
          "ENCRYPTION, ENCRYPTION",
          "DECRYPTION, DECRYPTION",
          "encrypt,    ENCRYPTION",
          "decrypt,    DECRYPTION",
          "EnCrYpT,    ENCRYPTION"
  })
  void fromString_ShouldReturnCorrectMode(String input, CrypticMode expected) {
    assertEquals(expected, CrypticMode.fromString(input));
  }

  @Test
  void fromString_ShouldThrowException_WhenModeIsUnknown() {
    String invalidMode = "unknown_mode";
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      CrypticMode.fromString(invalidMode);
    });

    assertTrue(exception.getMessage().contains(invalidMode));
  }
}