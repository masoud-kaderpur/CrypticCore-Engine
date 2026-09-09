package at.tuwien.crypticcore.core.domain;

/**
 * interface for byte-stream cipher operations.
 */
public interface CipherAlgorithm {

  /**
   * transforms a buffer chunk in-place starting at the given stream position.
   *
   * @param buffer the byte array containing raw data.
   * @param length number of valid bytes in the buffer to process.
   * @param key the secret key.
   * @param streamOffset current continuous byte offset.
   */
  void transform(byte[] buffer, int length, byte[] key, long streamOffset);

  /**
   * this method returns the name of the algorithm.
   *
   * @return algorithm name
   */
  String getName();
}