package at.tuwien.crypticcore.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * entry point for executing JMH performance benchmarks
 */
public class BenchmarkRunner {

  /**
   * builds execution options and triggers the JMH benchmark harness.
   *
   * @param args command-line arguments
   * @throws RunnerException if the benchmark execution fails
   */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(XorEngineBenchmark.class.getSimpleName())
        .build();

    new Runner(opt).run();
  }
}