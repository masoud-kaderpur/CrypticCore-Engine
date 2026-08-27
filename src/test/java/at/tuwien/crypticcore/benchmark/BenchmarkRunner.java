package at.tuwien.crypticcore.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry point for executing JMH performance benchmarks programmatically.
 * <p>Configures and runs {@link XorEngineBenchmark} without requiring
 * external command-line runner plugins.</p>
 */
public class BenchmarkRunner {

  /**
   * Builds execution options and triggers the JMH benchmark harness.
   *
   * @param args command-line arguments (unused)
   * @throws RunnerException if the benchmark execution fails
   */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(XorEngineBenchmark.class.getSimpleName())
        .build();

    new Runner(opt).run();
  }
}