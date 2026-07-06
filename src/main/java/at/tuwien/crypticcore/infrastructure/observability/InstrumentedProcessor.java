package at.tuwien.crypticcore.infrastructure.observability;

import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.StreamProcessor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;

/**
 * An architectural decorator that instruments file processing streams with production telemetry.
 *
 * <p>This component wraps a pure {@link StreamProcessor} instance, tracking loop latency
 * and processed byte counts via vendor-neutral metrics registries without polluting the core
 * cryptographic algorithms.</p>
 */
public class InstrumentedProcessor implements StreamProcessor {
  private final StreamProcessor delegate;
  private final MeterRegistry meterRegistry;

  /**
   * Constructs the metrics instrumentation decorator.
   *
   * @param delegate      the core domain stream processor doing the actual streaming trans.
   * @param meterRegistry the operational metrics framework registry
   */
  public InstrumentedProcessor(StreamProcessor delegate, MeterRegistry meterRegistry) {
    this.delegate = delegate;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void processFile(
      CrypticMode mode,
      String inputPath,
      String outputPath,
      byte[] key,
      long fileSize) throws IOException {

    // 1. Build infrastructure counters and timers isolated out of the engine core
    Counter bytesCounter = Counter.builder("crypticcore.bytes.processed")
        .description("Total number of bytes processed by the streaming engine")
        .tag("mode", mode.name())
        .register(meterRegistry);

    Timer latencyTimer = Timer.builder("crypticcore.transformation.latency")
        .description("Latency of the core streaming transformation loop")
        .tag("mode", mode.name())
        .register(meterRegistry);

    // 2. Start the telemetry stopwatch
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      // 3. Delegate the actual cryptographic work to the underlying pure processor
      delegate.processFile(mode, inputPath, outputPath, key, fileSize);

      // 4. Upon a successful, non-crashing run, track the full completed volume
      bytesCounter.increment(fileSize);

    } finally {
      // 5. Safely record performance duration metrics under all execution conditions
      sample.stop(latencyTimer);
    }
  }
}