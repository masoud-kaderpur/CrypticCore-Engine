package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.StreamProcessor;
import at.tuwien.crypticcore.core.engine.XorCipher;
import at.tuwien.crypticcore.core.engine.XorStreamProcessor;
import at.tuwien.crypticcore.infrastructure.io.ProgressObserver;
import at.tuwien.crypticcore.infrastructure.observability.InstrumentedProcessor;
import at.tuwien.crypticcore.infrastructure.observability.TelemetryServer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
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

    // 1. Initialize structural telemetry infrastructure
    PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    TelemetryServer telemetryServer = new TelemetryServer(8080, meterRegistry);
    telemetryServer.start();

    // 2. Extract application variables
    String modeArg = args[0];
    String input = args[1];
    String output = args[2];
    String password = args[3];
    String tempOutput = output + ".tmp";

    byte[] key = password.getBytes(StandardCharsets.UTF_8);

    // 3. Define presentation layer progress rendering
    ProgressObserver consoleObserver = percentage -> {
      System.out.print("\rProgress: [");
      int bars = percentage / 2;
      for (int i = 0; i < 50; i++) {
        System.out.print(i < bars ? "=" : " ");
      }
      System.out.print("] " + percentage + "%");
    };

    try {
      logger.info("Initializing execution pipeline for mode: {}", modeArg);

      // Parse domain models and configure structural payload parameters
      CrypticMode crypticMode = CrypticMode.fromString(modeArg);
      long rawSize = Files.size(Paths.get(input));
      long sizeForProgress = (crypticMode == CrypticMode.DECRYPTION)
          ? Math.max(0, rawSize - 4)
          : rawSize;

      if (crypticMode == CrypticMode.DECRYPTION && rawSize < 4) {
        throw new IllegalArgumentException("Invalid file: Too small for header.");
      }

      // 4. Construct behavioral processing matrix using strict Dependency Injection
      CipherAlgorithm xorCipher = new XorCipher();
      StreamProcessor pureEngine = new XorStreamProcessor(xorCipher, consoleObserver);
      StreamProcessor instrumentedProcessor = new InstrumentedProcessor(pureEngine, meterRegistry);

      // 5. Run operation combined with a micro-benchmark for CLI metrics printout
      long start = System.nanoTime();
      instrumentedProcessor.processFile(crypticMode, input, tempOutput, key, sizeForProgress);
      long end = System.nanoTime();

      // 6. Perform defensive atomic staging transfer
      Files.move(Paths.get(tempOutput), Paths.get(output), StandardCopyOption.REPLACE_EXISTING);

      // Render local CLI processing overview summary
      double seconds = (end - start) / 1_000_000_000.0;
      double megabytes = rawSize / (1024.0 * 1024.0);
      double throughput = megabytes / seconds;
      double durationMs = (end - start) / 1_000_000.0;

      logger.info("Operation completed successfully.");
      logger.info("----- Performance Statistics -----");
      logger.info("Action:      {}", crypticMode);
      logger.info("File Size:   {} MB", String.format("%.2f", megabytes));
      logger.info("Time taken:  {} ms", String.format("%.2f", durationMs));
      logger.info("Throughput:  {} MB/s", String.format("%.2f", throughput));

      logger.info("Holding engine alive for telemetry inspection. Press Ctrl+C to terminate.");
      Thread.currentThread().join();

    } catch (Exception e) {
      Files.deleteIfExists(Paths.get(tempOutput));
      logger.error("Critical error during processing: {}", e.getMessage());
    } finally {
      // 7. Secure cryptographic arrays erasure and clean up sockets
      Arrays.fill(key, (byte) 0);
      telemetryServer.stop();
    }
  }
}