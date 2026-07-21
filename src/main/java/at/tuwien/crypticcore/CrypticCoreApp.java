package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.EncryptionEngine;
import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.core.engine.XorEncryptionEngine;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import at.tuwien.crypticcore.infrastructure.telemetry.OpenTelemetryConfig;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line entry point for the CrypticCore encryption utility.
 * <p>This class manages the composition root of the application, orchestrating
 * dependency injection, argument parsing, telemetry setup, and atomic file replacement.</p>
 */
public class CrypticCoreApp {
  private static final Logger logger = LoggerFactory.getLogger(CrypticCoreApp.class);

  /**
   * Executes the cryptographic process based on command-line arguments.
   *
   * @param args containing: {@code <mode>}, {@code <input path>},
   *             {@code <output path>}, and {@code <password>}.
   */
  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("correct syntax: java -jar CrypticCore.jar <mode> <input> <output> <key>");
      System.exit(1);
      return;
    }

    OpenTelemetrySdk otelSdk = OpenTelemetryConfig.init();

    String mode = args[0];
    String input = args[1];
    String output = args[2];
    String password = args[3];
    String tempOutput = output + ".tmp";

    byte[] key = password.getBytes(StandardCharsets.UTF_8);

    try {
      logger.info("Initializing execution pipeline for mode: {}", mode);

      CrypticMode crypticMode = CrypticMode.fromString(mode);
      long size = Files.size(Paths.get(input));

      if (crypticMode == CrypticMode.DECRYPTION && size < 4) {
        throw new HeaderValidationException("Invalid file: Too small for CrypticCore header.");
      }

      CipherAlgorithm xorCipher = new XorCipher();
      EncryptionEngine processor = new XorEncryptionEngine(xorCipher);

      long start = System.nanoTime();
      processor.processFile(crypticMode, input, tempOutput, key, size);
      long end = System.nanoTime();

      Files.move(Paths.get(tempOutput), Paths.get(output), StandardCopyOption.REPLACE_EXISTING);

      double seconds = (end - start) / 1_000_000_000.0;
      double megabytes = size / (1024.0 * 1024.0);
      double throughput = megabytes / seconds;
      double durationMs = (end - start) / 1_000_000.0;

      logger.info("Operation completed successfully.");
      logger.info("----- Performance Statistics -----");
      logger.info("Action:      {}", crypticMode);
      logger.info("File Size:   {} MB", String.format("%.2f", megabytes));
      logger.info("Time taken:  {} ms", String.format("%.2f", durationMs));
      logger.info("Throughput:  {} MB/s", String.format("%.2f", throughput));

    } catch (ValidationException e) {
      cleanUpStagingFile(tempOutput);
      logger.error("Validation error: {}", e.getMessage());
    } catch (Exception e) {
      cleanUpStagingFile(tempOutput);
      logger.error("Critical error during processing: {}", e.getMessage(), e);
    } finally {
      Arrays.fill(key, (byte) 0);
      otelSdk.close();
    }
  }

  private static void cleanUpStagingFile(String tempPath) {
    try {
      Files.deleteIfExists(Paths.get(tempPath));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}