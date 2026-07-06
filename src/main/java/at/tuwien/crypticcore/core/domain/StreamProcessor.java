package at.tuwien.crypticcore.core.domain;

import java.io.IOException;

/**
 * Defines the core abstraction and behavioral contract for streaming file transformations
 * within the CrypticCore ecosystem.
 *
 * <p>Implementing architectures must guarantee O(1) space complexity by processing data
 * streams sequentially via bounded memory buffers. This abstraction completely decouples
 * the high-level orchestrator layers from specific cryptographic strategy implementations
 * and cross-cutting architectural concerns such as infrastructure metrics or logging.</p>
 */
public interface StreamProcessor {

  /**
   * Transforms an input file and streams the output to a specified destination path
   * using a cyclic cryptographic key schedule.
   *
   * @param mode       the execution mode determined by {@link CrypticMode} (ENCRYPTION/DECRYPTION)
   * @param inputPath  the fully qualified path string pointing to the source file
   * @param outputPath the fully qualified path string pointing to the destination file
   * @param key        the raw secret key array used for cyclic transformation; must not be empty
   * @param fileSize   the expected byte length of the input source file for integrity verification
   * @throws IOException              if any low-level file I/O operations block, if headers
   *                                  are structurally mismatched, or if data stream truncation
   *                                  is detected during verification
   * @throws IllegalArgumentException if any input parameters violate preconditions
   *                                  (e.g., empty keys, null parameters, identical path conf.)
   */
  void processFile(
      CrypticMode mode,
      String inputPath,
      String outputPath,
      byte[] key,
      long fileSize) throws IOException;
}