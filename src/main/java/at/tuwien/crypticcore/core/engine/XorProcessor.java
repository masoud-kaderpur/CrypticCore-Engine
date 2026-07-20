package at.tuwien.crypticcore.core.engine;

import static at.tuwien.crypticcore.infrastructure.io.FileValidator.isValidInputs;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.checkHeader;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.writeHeader;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.StreamProcessor;
import at.tuwien.crypticcore.infrastructure.io.ProgressObserver;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Core cryptographic stream processor implementation optimizing sequential file transformations.
 */
public class XorProcessor implements StreamProcessor {

  private final CipherAlgorithm algorithm;
  private final ProgressObserver observer;
  private final Tracer tracer;

  /**
   * Constructs the stream processor with explicit dependencies injected.
   *
   * @param algorithm the core single-byte transformation strategy to execute
   * @param observer  the UI decoupled interface loop tracking streaming lifecycle progress
   */
  public XorProcessor(CipherAlgorithm algorithm, ProgressObserver observer) {
    this.algorithm = algorithm;
    this.observer = observer;
    tracer = GlobalOpenTelemetry.getTracer("at.tuwien.crypticcore.core.engine", "1.0.0");
  }

  @Override
  public void processFile(
      CrypticMode mode,
      String inputPath,
      String outputPath,
      byte[] key,
      long fileSize) throws IOException {

    Span span = tracer.spanBuilder(mode.name().toLowerCase() + "_file")
        .setAttribute("cryptic.file.size", fileSize)
        .setAttribute("cryptic.file.input", inputPath)
        .setAttribute("cryptic.algorithm", algorithm.getClass().getSimpleName())
        .startSpan();

    span.addEvent("streaming_started");

    try {
      int bytesRead;
      int keyPointer = 0;
      int lastPercentage = -1;
      long totalBytesProcessed = 0;
      byte[] buffer = new byte[8192];

      isValidInputs(mode, key, Paths.get(inputPath), Paths.get(outputPath), fileSize);
      span.addEvent("inputs_verified");

      try (FileInputStream in = new FileInputStream(inputPath);
          FileOutputStream out = new FileOutputStream(outputPath)) {

        if (mode == CrypticMode.ENCRYPTION) {
          writeHeader(out);
          span.addEvent("header_written");
        } else {
          checkHeader(in);
          span.addEvent("header_verified");
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

      span.addEvent("streaming_completed");
      span.setStatus(StatusCode.OK);

    } catch (Exception e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }

  }
}