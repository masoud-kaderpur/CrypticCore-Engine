package at.tuwien.crypticcore.engine;

import at.tuwien.crypticcore.api.CipherAlgorithm;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Orchestrates file-based cryptographic operations using a streaming buffer approach.
 * <p>The engine handles file I/O, progress tracking, and metadata header validation
 * (Magic Bytes and Versioning) to  ensure data integrity across different versions of the
 * CrypticCore format.</p>
 */

public class EncryptionEngine {
  private final CipherAlgorithm algorithm;
  private static final byte[] MAGIC = "CCE".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  private static final byte VERSION = 1;

  /**
   * Initializes the engine with a specific cryptographic algorithm strategy. * @param algorithm
   * the strategy used for byte-level transformation
   */
  public EncryptionEngine(CipherAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Processes a file by applying the injected algorithm in either encryption or decryption mode.
   * <p>This method utilizes a 8192-byte buffer for memory efficiency and provides
   * real-time progress updates to the standard output. It enforces a strict file format by
   * reading/writing a custom header (CCE v1).</p> * * @param mode         the operation mode
   * (ENCRYPTION or DECRYPTION)
   *
   * @param inputPath  path to the source file
   * @param outputPath path where the processed data will be persisted
   * @param key        the byte array used for cyclic transformation; must not be empty
   * @param fileSize   the expected size of the input file for progress calculation and
   *                   integrity checks
   *
   * @throws IOException              if file access fails, a header mismatch occurs, or data
   *                                  truncation is detected
   * @throws IllegalArgumentException if parameters are null, paths overlap, or the key is invalid
   */
  public void processFile(CrypticMode mode,
                          String inputPath,
                          String outputPath,
                          byte[] key,
                          long fileSize) throws IOException {
    int bytesRead;
    int keyPointer = 0;
    int lastPercentage = -1;
    long totalBytesProcessed = 0;
    byte[] buffer = new byte[8192];

    validateParameters(mode, key, Paths.get(inputPath), Paths.get(outputPath), fileSize);

    try (FileInputStream in = new FileInputStream(inputPath);
         FileOutputStream out = new FileOutputStream(outputPath)) {
      if (mode == CrypticMode.ENCRYPTION) {
        writeHeader(out);
      } else {
        checkHeader(in);
      }

      while ((bytesRead = in.read(buffer)) != -1) {
        for (int i = 0; i < bytesRead; i++) {
          if (keyPointer == key.length) {
            keyPointer = 0;
          }
          buffer[i] = algorithm.transform(buffer[i], key[keyPointer++]);
        }
        out.write(buffer, 0, bytesRead);

        totalBytesProcessed += bytesRead;

        int currentPercentage = (int) ((totalBytesProcessed * 100L) / fileSize);

        if (currentPercentage > lastPercentage) {
          printProgress(currentPercentage);
          lastPercentage = currentPercentage;
        }
      }
      if (totalBytesProcessed != fileSize) {
        throw new IOException("Data truncation detected! Expected "
                + fileSize
                + " bytes but processed "
                + totalBytesProcessed);
      }
      System.out.println();
    }
  }

  /**
   * Validates the structural integrity of the input file by verifying the header.
   * <p>The header must consist of the "CCE" magic bytes followed by a
   * single-byte version identifier (currently {@value #VERSION}).</p> * @param in the input
   * stream positioned at the start of the file
   *
   * @throws IOException if the magic bytes do not match or the version is incompatible
   */
  private static void checkHeader(FileInputStream in) throws IOException {
    byte[] fileMagic = new byte[3];
    if (in.read(fileMagic) != 3 || !Arrays.equals(fileMagic, MAGIC)) {
      throw new IOException("Incorrect CrypticCore-Engine Data!");
    }
    int fileVersion = in.read();
    if (fileVersion != VERSION) {
      throw new IOException("Incompatible version: " + fileVersion);
    }
  }

  /**
   * Persists the CrypticCore metadata header to the output stream.
   * <p>This ensures that any future decryption attempts can identify the file
   * type and ensure version compatibility before processing data.</p> * @param out the output
   * stream to write the header to
   *
   * @throws IOException if the write operation fails
   */
  private static void writeHeader(FileOutputStream out) throws IOException {
    out.write(MAGIC);
    out.write(VERSION);
  }

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
  private static void validateParameters(CrypticMode mode,
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

  /**
   * Renders a synchronized progress bar to the standard output.
   * <p>Uses a carriage return (\r) to update the existing line, providing
   * a visual percentage-based status of the total bytes processed.</p> * @param percentage the
   * current completion state
   * (0-100)
   */
  private void printProgress(int percentage) {
    System.out.print("\rProgress: [");
    int bars = percentage / 2;
    for (int i = 0; i < 50; i++) {
      if (i < bars) {
        System.out.print("=");
      } else {
        System.out.print(" ");
      }
    }
    System.out.print("] " + percentage + "%");
  }
}