package at.tuwien.crypticcore.infrastructure.observability;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Technical infrastructure adapter managing the lifecycle of the embedded telemetry HTTP daemon.
 *
 * <p>This server maps metrics captured in the application registry
 * and exposes them through a standard text-based Prometheus scrape endpoint (/metrics),
 * cleanly separated from application routing logic.</p>
 */
public class TelemetryServer {
  private static final Logger logger = LoggerFactory.getLogger(TelemetryServer.class);

  private final int port;
  private final PrometheusMeterRegistry meterRegistry;
  private HttpServer server;

  /**
   * Instantiates the telemetry network listener.
   *
   * @param port          the network port number to bind the server socket to (e.g., 8080)
   * @param meterRegistry the registry provider containing runtime system metrics
   */
  public TelemetryServer(int port, PrometheusMeterRegistry meterRegistry) {
    this.port = port;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Initializes and boots the low-level HTTP network daemon.
   *
   * @throws RuntimeException if the network port binding configuration fails
   */
  public void start() {
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);

      server.createContext("/metrics", exchange -> {
        String response = meterRegistry.scrape();
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type",
            "text/plain; version=0.0.4; charset=utf-8");
        exchange.sendResponseHeaders(200, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
          os.write(responseBytes);
        }
      });

      server.start();
      logger.info("Prometheus metrics server successfully initialized on port {}", port);
    } catch (IOException e) {
      logger.error("Failed to bind telemetry HTTP server on port {}: {}", port, e.getMessage());
      throw new RuntimeException("Telemetry subsystem initialization failure", e);
    }
  }

  /**
   * Closes the server socket and performs a clean teardown of network threads.
   */
  public void stop() {
    if (server != null) {
      server.stop(1); // Give active requests 1 second to finish up safely
      logger.info("Telemetry server shut down cleanly.");
    }
  }
}