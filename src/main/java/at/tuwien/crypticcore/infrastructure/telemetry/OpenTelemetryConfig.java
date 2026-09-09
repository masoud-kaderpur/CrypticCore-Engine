package at.tuwien.crypticcore.infrastructure.telemetry;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * opentelemetry configuration.
 */
public class OpenTelemetryConfig {

  /**
   * this method initializes the otel sdk.
   *
   * @return configured {@link OpenTelemetrySdk} instance.
   */
  public static OpenTelemetrySdk init() {
    return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  }
}