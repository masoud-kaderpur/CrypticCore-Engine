package at.tuwien.crypticcore;


import at.tuwien.crypticcore.api.CipherAlgorithm;
import at.tuwien.crypticcore.engine.CrypticMode;
import at.tuwien.crypticcore.engine.EncryptionEngine;
import at.tuwien.crypticcore.engine.XorCipher;
import at.tuwien.crypticcore.io.ProgressObserver;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line entry point for the CrypticCore encryption utility.
 * <p>This class manages the lifecycle of a cryptographic operation, including
 * argument parsing, performance benchmarking, and atomic file replacement
 * via temporary staging files.</p>
 */
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(at.tuwien.crypticcore.Main.class);

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
      System.out.println("correct syntax: java -jar CrypticCore.jar <mode> <input> <output> "
              + "<key>");
      System.exit(1);
      return;
    }

    // meterregistry is managin the metrics
    PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // Create an HTTP server listening on port 8080
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    // Map the /metrics endpoint to scrape the Micrometer registry
    server.createContext("/metrics", exchange -> {
      // This formats the internal metrics into the official Prometheus text format
      String response = meterRegistry.scrape();

      exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
      byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, responseBytes.length);

      try (OutputStream os = exchange.getResponseBody()) {
        os.write(responseBytes);
      }
    });

    server.start();
    logger.info("Prometheus metrics endpoint live at http://localhost:8080/metrics");

    String mode = args[0];
    String input = args[1];
    String output = args[2];
    String password = args[3];
    String tempOutput = output + ".tmp";


    ProgressObserver consoleObserver = percentage -> {
      System.out.print("\rProgress: [");
      int bars = percentage / 2;
      for (int i = 0; i < 50; i++) {
        System.out.print(i < bars ? "=" : " ");
      }
      System.out.print("] " + percentage + "%");
    };

    CipherAlgorithm xor = new XorCipher();
    EncryptionEngine engine = new EncryptionEngine(xor, consoleObserver, meterRegistry);

    byte[] key = password.getBytes(StandardCharsets.UTF_8);

    try {
      logger.info("Initializing encryption engine for mode: {}", mode);

      CrypticMode crypticMode = CrypticMode.fromString(mode);
      long rawSize = Files.size(Paths.get(input));
      long sizeForProgress;

      if (crypticMode == CrypticMode.DECRYPTION) {
        if (rawSize < 4) {
          throw new IllegalArgumentException("Invalid file: Too small for header.");
        } else {
          sizeForProgress = rawSize - 4;
        }
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
      Arrays.fill(key, (byte) 0);
    }
  }
}