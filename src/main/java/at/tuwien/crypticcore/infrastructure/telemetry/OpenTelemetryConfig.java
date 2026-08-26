package at.tuwien.crypticcore.infrastructure.telemetry;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Global OpenTelemetry infrastructure configuration.
 */
public class OpenTelemetryConfig {

  /**
   * Initializes the OpenTelemetry SDK using environment properties.
   *
   * @return Fully configured {@link OpenTelemetrySdk} instance.
   */
  public static OpenTelemetrySdk init() {
    return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  }
}