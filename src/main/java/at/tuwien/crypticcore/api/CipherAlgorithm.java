package at.tuwien.crypticcore.api;

/**
 * Represents a functional strategy for single-byte cryptographic transformations.
 * <p>This interface serves as a contract for symmetric or asymmetric primitive
 * operations where a data unit is transformed using a specific key unit.
 * Implementations must ensure that the transformation
 * is deterministic for any given pair of inputs.</p>
 */
@FunctionalInterface
public interface CipherAlgorithm {
  /**
   * Transforms a single byte of data using the provided key byte.
   * <p>This method defines the core atomic operation of the cipher. Depending
   * on the implementation, this may represent an encryption, decryption,
   * or obfuscation step (e.g., a XOR-based stream cipher or a substitution box).</p>
   *
   * @param data the byte to be transformed; usually represents plaintext or ciphertext
   * @param key  the byte used to influence the transformation logic
   *
   * @return the resulting transformed byte
   *
   * @throws ArithmeticException if the transformation encounters  an unrecoverable mathematical
   *                             error
   * @apiNote In high-performance scenarios, consider the overhead of primitive boxing.
   */
  byte transform(byte data, byte key);
}