package at.tuwien.crypticcore.core.domain;

/**
 * Strategy interface for byte-stream cipher operations.
 */
public interface CipherAlgorithm {

  /**
   * Transforms a buffer chunk in-place starting at the given stream position.
   *
   * @param buffer the byte array containing raw data
   * @param length number of valid bytes in the buffer to process
   * @param key the secret key material
   * @param streamOffset current continuous byte offset in the overall stream
   */
  void transform(byte[] buffer, int length, byte[] key, long streamOffset);

  /**
   * Returns the canonical name of the algorithm.
   *
   * @return algorithm identifier
   */
  String getName();
}