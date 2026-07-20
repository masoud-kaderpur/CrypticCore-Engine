package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.Processor;
import at.tuwien.crypticcore.core.engine.XorCipher;
import at.tuwien.crypticcore.core.engine.XorProcessor;
import at.tuwien.crypticcore.infrastructure.observability.OpenTelemetryConfig;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
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
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

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
   *
   * @throws IOException if file system operations or stream processing fails.
   */
  public static void main(String[] args) throws IOException {
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
        throw new IllegalArgumentException("Invalid file: Too small for header.");
      }

      CipherAlgorithm xorCipher = new XorCipher();
      Processor processor = new XorProcessor(xorCipher);

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

      logger.info("Holding engine alive for telemetry inspection. Press Ctrl+C to terminate.");

      try {
        Thread.currentThread().join();
      } catch (InterruptedException e) {
        logger.info("Termination signal received.");
        Thread.currentThread().interrupt();
      }

    } catch (Exception e) {
      Files.deleteIfExists(Paths.get(tempOutput));
      logger.error("Critical error during processing: {}", e.getMessage());
    } finally {
      Arrays.fill(key, (byte) 0);
    }
    otelSdk.close();
  }
}