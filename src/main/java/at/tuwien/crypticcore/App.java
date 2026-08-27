package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.infrastructure.telemetry.OpenTelemetryConfig;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * command line entry point for the engine.
 */
public class App {
  /**
   * calls the executor based on command line arguments.
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.exit(1);
      return;
    }

    CrypticMode mode = CrypticMode.fromString(args[0]);
    Path inputPath = Path.of(args[1]);
    Path outputPath = Path.of(args[2]);
    Path tempOutPath = Path.of(args[2] + ".tmp");
    byte[] key = args[3].getBytes(StandardCharsets.UTF_8);
    long fileSize = Files.size(inputPath);

    Context context = new Context(mode, inputPath, outputPath, tempOutPath, key, fileSize);

    OpenTelemetrySdk otelSdk = OpenTelemetryConfig.init();

    try (otelSdk) {
      Executor.execute(context);
    } catch (Exception e) {
      System.exit(1);
    }
  }
}