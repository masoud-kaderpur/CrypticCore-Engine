package at.tuwien.crypticcore.core.engine;

import static at.tuwien.crypticcore.infrastructure.io.FileValidator.isValidInputs;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.checkHeader;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.writeHeader;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.CrypticMode;
import at.tuwien.crypticcore.core.domain.Processor;
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
public class XorProcessor implements Processor {

  private final CipherAlgorithm algorithm;
  private final Tracer tracer;

  public XorProcessor(CipherAlgorithm algorithm) {
    this.algorithm = algorithm;
    this.tracer = GlobalOpenTelemetry.getTracer("at.tuwien.crypticcore.core.engine", "1.0.0");
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
        }
      }

      if (mode == CrypticMode.ENCRYPTION) {
        if (totalBytesProcessed != fileSize) {
          throw new IOException("Data truncation during encryption! Expected to process "
              + fileSize + " bytes of payload, but processed " + totalBytesProcessed);
        }
      } else {
        long totalReadWithHeader = totalBytesProcessed + 4;
        if (totalReadWithHeader != fileSize) {
          throw new IOException("Data truncation during decryption! Encrypted file size is "
              + fileSize + " bytes, but we only accounted for " + totalReadWithHeader + " bytes.");
        }
      }

      span.addEvent("streaming_completed");
      span.setStatus(StatusCode.OK);

    } catch (IOException | RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }
}