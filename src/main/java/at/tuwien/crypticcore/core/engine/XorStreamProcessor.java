package at.tuwien.crypticcore.core.engine;

import static at.tuwien.crypticcore.infrastructure.io.FileValidator.isValidInputs;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.checkHeader;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.writeHeader;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.StreamProcessor;
import at.tuwien.crypticcore.infrastructure.io.ProgressObserver;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Core cryptographic stream processor implementation optimizing sequential file transformations.
 *
 * <p>This implementation guarantees strict Single Responsibility adherence by managing only the
 * sequential streaming lifecycle, buffering mechanics, and basic verification assertions.
 * It maintains an absolute separation from cross-cutting metrics compilation infrastructure.</p>
 */
public class XorStreamProcessor implements StreamProcessor {

  private final CipherAlgorithm algorithm;
  private final ProgressObserver observer;

  /**
   * Constructs the stream processor with explicit dependencies injected.
   *
   * @param algorithm the core single-byte transformation strategy to execute
   * @param observer  the UI decoupled interface loop tracking streaming lifecycle progress
   */
  public XorStreamProcessor(CipherAlgorithm algorithm, ProgressObserver observer) {
    this.algorithm = algorithm;
    this.observer = observer;
  }

  @Override
  public void processFile(
      CrypticMode mode,
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

        totalBytesProcessed += bytesRead;
        int currentPercentage = (int) ((totalBytesProcessed * 100L) / fileSize);
        if (currentPercentage > lastPercentage && observer != null) {
          observer.onProgressUpdate(currentPercentage);
          lastPercentage = currentPercentage;
        }
      }
    }

    if (totalBytesProcessed != fileSize) {
      throw new IOException("Data truncation detected! Expected "
          + fileSize
          + " bytes but processed "
          + totalBytesProcessed);
    }
  }
}