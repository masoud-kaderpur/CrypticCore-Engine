package at.tuwien.crypticcore.core.engine.algorithm;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import java.util.Arrays;

/**
 * High-performance involutory XOR cipher implementation using bulk array processing.
 */
public class XorCipher implements CipherAlgorithm {

  private static final String NAME = "XOR";

  @Override
  public void transform(byte[] buffer, int length, byte[] key, long streamOffset) {
    int keyLength = key.length;

    for (int i = 0; i < length; i++) {
      int keyIndex = (int) ((streamOffset + i) % keyLength);
      buffer[i] = (byte) (buffer[i] ^ key[keyIndex]);
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  /**
   * Overwrites key material with zeroes to sanitize heap memory.
   *
   * @param key sensitive key material to wipe
   */
  public static void wipeMemory(byte[] key) {
    if (key != null) {
      Arrays.fill(key, (byte) 0);
    }
  }
}