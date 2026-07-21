package at.tuwien.crypticcore.benchmark;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class XorEngineBenchmark {

  private CipherAlgorithm xorCipher;
  private byte[] key;
  private byte[] dataBuffer;

  @Param({"1024", "8192", "65536"})
  private int bufferSize;

  @Setup(Level.Trial)
  public void setup() {
    this.xorCipher = new XorCipher();
    this.key = "SecretBenchmarkKey123".getBytes(StandardCharsets.UTF_8);
    this.dataBuffer = new byte[bufferSize];

    new Random(42).nextBytes(dataBuffer);
  }

  @Benchmark
  public byte[] benchmarkBulkArrayTransform() {
    xorCipher.transform(dataBuffer, dataBuffer.length, key, 0);
    return dataBuffer;
  }

  @Benchmark
  public byte[] benchmarkSingleByteTransformNaive() {
    for (int i = 0; i < dataBuffer.length; i++) {
      dataBuffer[i] = (byte) (dataBuffer[i] ^ key[i % key.length]);
    }
    return dataBuffer;
  }
}