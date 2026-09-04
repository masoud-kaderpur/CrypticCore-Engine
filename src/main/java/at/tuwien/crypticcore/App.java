package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.infrastructure.telemetry.OpenTelemetryConfig;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * command line entry point for the engine.
 */
public class App {

  private static final String USAGE =
      "Usage: java -jar CrypticCore.jar <ENCRYPTION|DECRYPTION> <input_file> <output_file> <key>";

  /**
   * parses arguments and dispatches execution to the Executor.
   *
   * @param args CLI arguments
   */
  public static void main(String[] args) {
    if (args.length != 4) {
      System.err.println(USAGE);
      System.exit(1);
    }

    try {
      CrypticMode mode = CrypticMode.fromString(args[0]);
      Path inputPath = Path.of(args[1]);
      Path outputPath = Path.of(args[2]);
      Path tempOutPath = Path.of(args[2] + ".tmp");
      byte[] key = args[3].getBytes(StandardCharsets.UTF_8);
      long fileSize = Files.exists(inputPath) ? Files.size(inputPath) : 0L;

      Context context = new Context(mode, inputPath, outputPath, tempOutPath, key, fileSize);

      OpenTelemetrySdk otelSdk = OpenTelemetryConfig.init();
      try (otelSdk) {
        String version = App.class.getPackage().getImplementationVersion();
        Tracer tracer = otelSdk.getTracer(
            "at.tuwien.crypticcore.core.engine",
            version != null ? version : "dev"
        );

        Executor.execute(context, tracer);
      }
    } catch (Exception e) {
      System.err.println("Execution failed: " + e.getMessage());
      System.exit(1);
    }
  }
}