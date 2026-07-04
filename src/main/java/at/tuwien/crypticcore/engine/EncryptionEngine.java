package at.tuwien.crypticcore.engine;

import static at.tuwien.crypticcore.io.FileValidator.isValidInputs;
import static at.tuwien.crypticcore.io.HeaderHandler.checkHeader;
import static at.tuwien.crypticcore.io.HeaderHandler.writeHeader;

import at.tuwien.crypticcore.api.CipherAlgorithm;
import at.tuwien.crypticcore.io.HeaderHandler;
import at.tuwien.crypticcore.io.ProgressObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * High-performance streaming engine for cryptographic file transformations.
 * <p>The engine orchestrates the data flow by reading from an input stream,
 * applying a {@link CipherAlgorithm}, and writing to an output stream.
 * It remains decoupled from specific file formats and UI representations
 * through the use of {@link HeaderHandler} and {@link ProgressObserver}.</p>
 */
public class EncryptionEngine {
  private final CipherAlgorithm algorithm;
  private final ProgressObserver observer;
  private final MeterRegistry meterRegistry;

  /**
   * Initializes the engine with a specific cryptographic algorithm strategy.
   *
   * @param algorithm the cryptographic strategy to be applied
   * @param observer  the callback for monitoring processing progress
   */
  public EncryptionEngine(
      CipherAlgorithm algorithm,
      ProgressObserver observer,
      MeterRegistry meterRegistry) {
    this.algorithm = algorithm;
    this.observer = observer;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Processes a file by applying the injected algorithm in either encryption or decryption mode.
   * <p>This method utilizes a 8192-byte buffer for memory efficiency and provides
   * real-time progress updates to the standard output. It enforces a strict file format by
   * reading/writing a custom header (CCE v1).</p>
   *
   * @param mode       the operation mode
   *                   (ENCRYPTION or DECRYPTION)
   * @param inputPath  path to the source file
   * @param outputPath spath where the processed data will be persisted
   * @param key        the byte array used for cyclic transformation; must not be empty
   * @param fileSize   the expected size of the input file for progress calculation and
   *                   integrity checks
   *
   * @throws IOException              if file access fails, a header mismatch occurs, or data
   *                                  truncation is detected
   * @throws IllegalArgumentException if parameters are null, paths overlap, or the key is invalid
   */
  public void processFile(CrypticMode mode,
                          String inputPath,
                          String outputPath,
                          byte[] key,
                          long fileSize) throws IOException {
    int bytesRead;
    int keyPointer = 0;
    int lastPercentage = -1;
    long totalBytesProcessed = 0;
    byte[] buffer = new byte[8192];

    isValidInputs(mode, key, Paths.get(inputPath), Paths.get(outputPath), fileSize);

    // bytes counter named "crypticcore.bytes.processed" (draft)
    // structural Tag to distinguish between ENCRYPTION and DECRYPTION in dashboards
    Counter bytesCounter = Counter.builder("crypticcore.bytes.processed")
        .description("Total number of bytes processed by the streaming engine")
        .tag("mode", mode.name())
        .register(meterRegistry);

    // registration of the timer
    Timer latencyTimer = Timer.builder("crypticcore.transformation.latency")
        .description("Latency of the core streaming transformation loop")
        .tag("mode", mode.name())
        .register(meterRegistry);

    // starting the stopwatch
    Timer.Sample sample = Timer.start(meterRegistry);

    try (FileInputStream in = new FileInputStream(inputPath);
         FileOutputStream out = new FileOutputStream(outputPath)) {
      if (mode == CrypticMode.ENCRYPTION) {
        writeHeader(out);
      } else {
        checkHeader(in);
      }

      while ((bytesRead = in.read(buffer)) != -1) {
        for (int i = 0; i < bytesRead; i++) {
          if (keyPointer == key.length) {
            keyPointer = 0;
          }
          buffer[i] = algorithm.transform(buffer[i], key[keyPointer++]);
        }
        out.write(buffer, 0, bytesRead);
        // increment the counter by the exact number of bytes processed in this block
        bytesCounter.increment(bytesRead);
        totalBytesProcessed += bytesRead;
        int currentPercentage = (int) ((totalBytesProcessed * 100L) / fileSize);
        if (currentPercentage > lastPercentage && observer != null) {
          observer.onProgressUpdate(currentPercentage);
          lastPercentage = currentPercentage;
        }
      }
    }

    // stoping the stopwatch
    sample.stop(latencyTimer);

    if (totalBytesProcessed != fileSize) {
      throw new IOException("Data truncation detected! Expected "
              + fileSize
              + " bytes but processed "
              + totalBytesProcessed);
    }
  }
}
