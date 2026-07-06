package at.tuwien.crypticcore.infrastructure.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class TelemetryServerTest {

  @Test
  void testTelemetryServer_FullLifecycleAndScrape() throws Exception {
    PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    // Use an isolated test port (8081) to prevent conflicts with standard execution
    TelemetryServer server = new TelemetryServer(8081, meterRegistry);

    // Increment a dummy metric to ensure there's trace data to scrape
    meterRegistry.counter("test.metric").increment();

    try {
      server.start();

      // Set up an isolated, lightweight HTTP client to scrape our server
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("http://localhost:8081/metrics"))
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      // Assert infrastructure transport criteria
      assertEquals(200, response.statusCode(), "Scrape endpoint must respond with HTTP 200 OK");
      assertTrue(response.headers().firstValue("Content-Type").isPresent());
      assertTrue(response.headers().firstValue("Content-Type").get().contains("text/plain"),
          "Must serve the specific format expected by Prometheus");

      // Assert that the registry metrics data payload matches what the server outputs
      String expectedScrapeData = meterRegistry.scrape();
      assertEquals(expectedScrapeData, response.body(), "Scraped payload must match the registry internal status");

    } finally {
      server.stop();
    }
  }

  @Test
  void testStart_ShouldThrowRuntimeException_WhenPortIsAlreadyBound() {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    TelemetryServer primaryServer = new TelemetryServer(8082, registry);
    TelemetryServer clashingServer = new TelemetryServer(8082, registry);

    try {
      primaryServer.start();

      // Running the second server on the same port should force a bind exception
      RuntimeException exception = assertThrows(RuntimeException.class, clashingServer::start);

      assertTrue(exception.getMessage().contains("Telemetry subsystem initialization failure"),
          "Should report specific architectural initialization failure errors");

    } finally {
      primaryServer.stop();
      clashingServer.stop();
    }
  }
}