package at.tuwien.crypticcore.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CrypticMode Enum Tests")
class CrypticModeTest {

  @Nested
  @DisplayName("Successful Parsing Tests")
  class ValidParsingTests {

    @ParameterizedTest
    @ValueSource(strings = {"ENCRYPTION", "encryption", "encrypt", "  encrypt  ", "Encrypt", "EnCrYpTiOn"})
    @DisplayName("Should successfully parse all ENCRYPTION variants and aliases")
    void shouldParseEncryptionVariants(String input) {
      CrypticMode mode = CrypticMode.fromString(input);
      assertThat(mode).isEqualTo(CrypticMode.ENCRYPTION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DECRYPTION", "decryption", "decrypt", "  decrypt  ", "Decrypt", "DeCrYpTiOn"})
    @DisplayName("Should successfully parse all DECRYPTION variants and aliases")
    void shouldParseDecryptionVariants(String input) {
      CrypticMode mode = CrypticMode.fromString(input);
      assertThat(mode).isEqualTo(CrypticMode.DECRYPTION);
    }
  }

  @Nested
  @DisplayName("Invalid Input & Exception Handling Tests")
  class InvalidParsingTests {

    @Test
    @DisplayName("Should throw IllegalArgumentException when input is null")
    void shouldThrowExceptionOnNullInput() {
      assertThatThrownBy(() -> CrypticMode.fromString(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Mode input text cannot be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "invalid", "cipher", "xorencrypt"})
    @DisplayName("Should throw IllegalArgumentException for unknown modes or empty strings")
    void shouldThrowExceptionOnUnknownInput(String input) {
      assertThatThrownBy(() -> CrypticMode.fromString(input))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unknown mode: " + input);
    }
  }
}