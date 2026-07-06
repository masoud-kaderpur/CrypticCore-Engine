package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.engine.XorCipher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Technical verification suite for the {@link XorCipher} implementation.
 * <p>Focuses on verifying algebraic properties of the XOR operation,
 * specifically its behavior as an involution and its handling of
 * signed/unsigned byte boundaries.</p>
 */
@DisplayName("Unit Tests: Cryptographic Logic")
class XorCipherTest {

  private final CipherAlgorithm algorithm = new XorCipher();

  /**
   * Verifies the involutory property of the XOR cipher.
   * <p>Ensures that the transformation is its own inverse, satisfying
   * the condition {@code f(f(x, k), k) == x}. This is the core
   * requirement for symmetric encryption/decryption consistency.</p>
   */
  @Test
  @DisplayName("Should correctly encrypt and decrypt (Involution)")
  void testInvolution() {
    byte original = 0x41;
    byte key = 0x2A;
    byte encrypted = algorithm.transform(original, key);

    assertNotEquals(original, encrypted, "Ciphertext must differ from Plaintext");
    assertEquals(original, algorithm.transform(encrypted, key), "Double XOR must return original");
  }

  @ParameterizedTest
  @ValueSource(bytes = {0, -1, 127, -128})
  @DisplayName("Edge Cases: Extreme byte values and signs")
  void testEdgeCaseBytes(byte b) {
    byte key = (byte) 0xAA;
    byte encrypted = algorithm.transform(b, key);
    assertEquals(b, algorithm.transform(encrypted, key), "Failed at byte: " + b);
  }
  
  @Test
  @DisplayName("Identity: XOR with 0 should not change data")
  void testZeroKey() {
    byte data = 0x55;
    assertEquals(data, algorithm.transform(data, (byte) 0));
  }
}