package at.tuwien.crypticcore.io;

import at.tuwien.crypticcore.engine.CrypticMode;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for pre-flight file system validation.
 * <p>Provides defensive checks to ensure that input files exist and
 * output paths are valid before initiating resource-heavy I/O operations.</p>
 */
public class FileValidator {
  /**
   * Performs a pre-flight check on all operation parameters to ensure fail-fast behavior before
   * initiating heavy I/O. *
   *
   * @param mode     the selected cryptographic mode
   * @param key      the byte array used for transformation
   * @param inPath   the resolved path of the input file
   * @param outPath  the resolved path of the output file
   * @param fileSize the size of the file to be processed
   *
   * @throws FileNotFoundException    if the input path cannot be resolved
   * @throws IllegalArgumentException if parameters are null, the key is empty, or input and
   *                                  output paths are identical
   */
  public static void isValidInputs(CrypticMode mode,
                                   byte[] key,
                                   Path inPath,
                                   Path outPath,
                                   long fileSize) throws IOException {
    if (mode == null) {
      throw new IllegalArgumentException("Mode must not be null");
    }

    if (!Files.exists(inPath)) {
      throw new FileNotFoundException("Input file does not exist: " + inPath);
    }

    if (Files.exists(outPath) && Files.isSameFile(inPath, outPath)) {
      throw new IllegalArgumentException("Input and output paths must not be the same!");
    }

    if (key == null || key.length == 0) {
      throw new IllegalArgumentException("Key must not be null or empty");
    }

    if (fileSize == 0) {
      throw new IllegalArgumentException("File size must be >0");
    }
  }
}