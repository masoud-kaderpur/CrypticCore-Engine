package at.tuwien.crypticcore.engine;

import at.tuwien.crypticcore.api.CipherAlgorithm;

/**
 * An implementation of {@link CipherAlgorithm} using the bitwise XOR (Exclusive OR) operation.
 * <p>The XOR cipher is a symmetric additive cipher. Due to the mathematical property
 * {@code (a ^ b) ^ b = a}, this implementation is involutory, meaning the same
 * transformation logic is used for both encryption and decryption.</p>
 */
public class XorCipher implements CipherAlgorithm {
  /**
   * Applies the XOR bitwise operation to the input data byte using the provided key.
   * <p>The operation is performed by masking the signed bytes to {@code 0xFF}
   * to treat them as unsigned values before the XOR operation, ensuring
   * consistent bitwise behavior.</p>
   * *
   * * @param data the byte to be transformed
   *
   * @param key the byte used as the XOR mask
   * @return the result of {@code data ^ key} cast back to a byte
   */
  @Override
  public byte transform(byte data, byte key) {
    return (byte) ((data & 0xFF) ^ (key & 0xFF));
  }
}