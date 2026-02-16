package at.tuwien.crypticcore;

import at.tuwien.crypticcore.engine.CrypticMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class CrypticModeTest {

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