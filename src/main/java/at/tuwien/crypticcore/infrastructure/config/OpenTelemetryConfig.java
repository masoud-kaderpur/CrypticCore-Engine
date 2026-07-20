package at.tuwien.crypticcore.infrastructure.config;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Global OpenTelemetry infrastructure configuration.
 * Uses zero-code autoconfiguration to manage telemetry via environment variables.
 */
public class OpenTelemetryConfig {

  /**
   * Initializes the OpenTelemetry SDK using environment properties.
   *
   * @return Fully configured {@link OpenTelemetrySdk} instance.
   *         Note: For short-lived CLI apps, this instance must be closed via .close()
   *         at application exit to flush remaining spans and prevent data loss.
   */
  public static OpenTelemetrySdk init() {
    // https://opentelemetry.io/docs/languages/java/configuration/#zero-code-sdk-autoconfigure
    return AutoConfiguredOpenTelemetrySdk.initialize()
        .getOpenTelemetrySdk();
  }
}