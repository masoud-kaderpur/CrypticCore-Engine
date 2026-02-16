package at.tuwien.crypticcore;


import at.tuwien.crypticcore.api.CipherAlgorithm;
import at.tuwien.crypticcore.engine.CrypticMode;
import at.tuwien.crypticcore.engine.EncryptionEngine;
import at.tuwien.crypticcore.engine.XorCipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Command-line entry point for the CrypticCore encryption utility.
 * <p>This class manages the lifecycle of a cryptographic operation, including
 * argument parsing, performance benchmarking, and atomic file replacement
 * via temporary staging files.</p>
 */
public class Main {
  /**
   * Executes the cryptographic process based on command-line arguments.
   * <p>The workflow follows these stages:
   * <ol>
   * <li>Argument validation and key derivation.</li>
   * <li>Pre-calculation of payload size for accurate progress tracking.</li>
   * <li>Transformation to a {@code .tmp} staging file to prevent data corruption.</li>
   * <li>Atomic move of the staging file to the final destination.</li>
   * <li>Security cleanup of sensitive key material.</li>
   * </ol></p>
   * *
   *
   * @param args Array containing: {@code <mode>}, {@code <input path>},
   *             {@code <output path>}, and {@code <password>}.
   * @throws IOException if file system operations or stream processing fails.
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.out.println("correct syntax: java -jar CrypticCore.jar <mode> <input> <output> <key>");
      System.exit(1);
      return;
    }

    String mode = args[0], input = args[1], output = args[2], password = args[3], tempOutput = output + ".tmp";

    CipherAlgorithm xor = new XorCipher();
    EncryptionEngine engine = new EncryptionEngine(xor);

    byte[] key = password.getBytes(StandardCharsets.UTF_8);

    try {
      CrypticMode crypticMode = CrypticMode.fromString(mode);
      long rawSize = Files.size(Paths.get(input)), sizeForProgress;
      if (crypticMode == CrypticMode.DECRYPTION) {
        if (rawSize < 4) throw new IllegalArgumentException("Invalid file: Too small for header.");
        else sizeForProgress = rawSize - 4;
      } else {
        sizeForProgress = rawSize;
      }

      long start = System.nanoTime();
      engine.processFile(crypticMode, input, tempOutput, key, sizeForProgress);
      long end = System.nanoTime();

      Files.move(Paths.get(tempOutput), Paths.get(output), StandardCopyOption.REPLACE_EXISTING);

      long durationNs = end - start;
      double seconds = durationNs / 1_000_000_000.0;
      double megabytes = rawSize / (1024.0 * 1024.0);
      double throughput = megabytes / seconds;
      double durationMs = durationNs / 1_000_000.0;


      System.out.println("\n----- Performance Statistics -----");
      System.out.println();
      System.out.printf("  Action:      %s%n", crypticMode);
      System.out.printf("  File Size:   %.2f MB%n", megabytes);
      System.out.printf("  Time taken:  %.2f ms%n", durationMs);
      System.out.printf("  Throughput:  %.2f MB/s%n", throughput);
      System.out.println();
      System.out.println("------------------------------------");


    } catch (Exception e) {
      Files.deleteIfExists(Paths.get(tempOutput));
      System.out.println("Error:" + e.getMessage());
    } finally {
      Arrays.fill(key, (byte) 0);
    }
  }
}