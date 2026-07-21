package at.tuwien.crypticcore.infrastructure.io;

import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for pre-flight file system validation.
 * <p>Provides defensive checks to ensure that input files exist and
 * output paths are valid before initiating resource-heavy I/O operations.</p>
 */
public class FileValidator {

  private FileValidator() {
    // Utility class
  }

  /**
   * Performs a pre-flight check on all operation parameters to ensure fail-fast behavior.
   *
   * @param mode     the selected cryptographic mode
   * @param key      the byte array used for transformation
   * @param inPath   the resolved path of the input file
   * @param outPath  the resolved path of the output file
   * @param fileSize the size of the file to be processed
   * @throws ValidationException if inputs fail validation checks
   * @throws IOException         if file path comparison fails
   */
  public static void isValidInputs(CrypticMode mode,
      byte[] key,
      Path inPath,
      Path outPath,
      long fileSize) throws IOException, ValidationException {
    if (mode == null) {
      throw new ValidationException("Mode must not be null");
    }

    if (!Files.exists(inPath)) {
      throw new ValidationException("Input file does not exist: " + inPath);
    }

    if (Files.exists(outPath) && Files.isSameFile(inPath, outPath)) {
      throw new ValidationException("Input and output paths must not be the same!");
    }

    if (key == null || key.length == 0) {
      throw new ValidationException("Key must not be null or empty");
    }

    if (fileSize <= 0) {
      throw new ValidationException("File size must be greater than 0");
    }
  }
}