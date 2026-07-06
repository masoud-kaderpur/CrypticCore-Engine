package at.tuwien.crypticcore.infrastructure.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.StreamProcessor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstrumentedProcessorTest {

  private SimpleMeterRegistry meterRegistry;
  private MockStreamProcessor mockDelegate;
  private InstrumentedProcessor instrumentedProcessor;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    mockDelegate = new MockStreamProcessor();
    instrumentedProcessor = new InstrumentedProcessor(mockDelegate, meterRegistry);
  }

  @Test
  void testProcessFile_SuccessfulRun_ShouldRecordMetrics() throws IOException {
    long expectedSize = 500L;

    // Execute decorator
    instrumentedProcessor.processFile(CrypticMode.ENCRYPTION, "in.txt", "out.cce", new byte[]{1}, expectedSize);

    // Verify underlying engine was called
    assertEquals(1, mockDelegate.callCount, "Delegate processor must be executed");

    // Verify throughput counter logic
    Counter counter = meterRegistry.find("crypticcore.bytes.processed")
        .tag("mode", "ENCRYPTION")
        .counter();
    assert counter != null;
    assertEquals(500.0, counter.count(), "Byte counter must register exact payload metrics matching file size");

    // Verify stopwatch timer logic
    Timer timer = meterRegistry.find("crypticcore.transformation.latency")
        .tag("mode", "ENCRYPTION")
        .timer();
    assert timer != null;
    assertEquals(1, timer.count(), "Performance stopwatch tracker must capture exactly one complete execution window");
  }

  @Test
  void testProcessFile_ExceptionalRun_ShouldStillRecordLatencyButSkipBytes() {
    mockDelegate.shouldThrow = true;
    long expectedSize = 500L;

    // Assert that the exception from the engine propagates cleanly
    assertThrows(IOException.class, () -> {
      instrumentedProcessor.processFile(CrypticMode.ENCRYPTION, "in.txt", "out.cce", new byte[]{1}, expectedSize);
    });

    // Verify counter skipped processing due to application crash
    Counter counter = meterRegistry.find("crypticcore.bytes.processed")
        .tag("mode", "ENCRYPTION")
        .counter();
    assert counter != null;
    assertEquals(0.0, counter.count(), "Byte counter must not increment on aborted/failed streaming pipelines");

    // Verify timer still managed to log the lifespan run inside the finally block
    Timer timer = meterRegistry.find("crypticcore.transformation.latency")
        .tag("mode", "ENCRYPTION")
        .timer();
    assert timer != null;
    assertEquals(1, timer.count(), "Latency timer must capture the lifespan run even under broken engine environments");
  }

  // Pure decoupled mock engine to simulate real infrastructure behaviors without disk requirements
  private static class MockStreamProcessor implements StreamProcessor {
    int callCount = 0;
    boolean shouldThrow = false;

    @Override
    public void processFile(CrypticMode mode, String in, String out, byte[] key, long size) throws IOException {
      callCount++;
      if (shouldThrow) {
        throw new IOException("Simulated streaming pipeline failure");
      }
    }
  }
}