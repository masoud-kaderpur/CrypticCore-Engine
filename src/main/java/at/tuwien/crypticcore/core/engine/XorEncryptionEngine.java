package at.tuwien.crypticcore.core.engine;

import static at.tuwien.crypticcore.infrastructure.io.FileValidator.isValidInputs;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.checkHeader;
import static at.tuwien.crypticcore.infrastructure.io.HeaderHandler.writeHeader;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.EncryptionEngine;
import at.tuwien.crypticcore.core.domain.exception.CrypticException;
import at.tuwien.crypticcore.core.domain.exception.DataTruncationException;
import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
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
public class XorEncryptionEngine implements EncryptionEngine {

  private static final int BUFFER_SIZE = 8192;
  private final CipherAlgorithm algorithm;
  private final Tracer tracer;

  public XorEncryptionEngine(CipherAlgorithm algorithm) {
    this.algorithm = algorithm;
    this.tracer = GlobalOpenTelemetry.getTracer("at.tuwien.crypticcore.core.engine", "1.0.0");
  }

  @Override
  public void processFile(
      CrypticMode mode,
      String inputPath,
      String outputPath,
      byte[] key,
      long fileSize) throws IOException, ValidationException {

    Span span = tracer.spanBuilder(mode.name().toLowerCase() + "_file")
        .setAttribute("cryptic.file.size", fileSize)
        .setAttribute("cryptic.file.input", inputPath)
        .setAttribute("cryptic.algorithm", algorithm.getName())
        .startSpan();

    span.addEvent("streaming_started");

    try {
      int bytesRead;
      long totalBytesProcessed = 0;
      byte[] buffer = new byte[BUFFER_SIZE];

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
          algorithm.transform(buffer, bytesRead, key, totalBytesProcessed);
          out.write(buffer, 0, bytesRead);
          totalBytesProcessed += bytesRead;
        }
      }

      if (mode == CrypticMode.ENCRYPTION) {
        if (totalBytesProcessed != fileSize) {
          throw new DataTruncationException("Data truncation during encryption! Expected: "
              + fileSize + " bytes, processed: " + totalBytesProcessed);
        }
      } else {
        long totalReadWithHeader = totalBytesProcessed + 4;
        if (totalReadWithHeader != fileSize) {
          throw new DataTruncationException("Data truncation during decryption! Expected: "
              + fileSize + " bytes, accounted: " + totalReadWithHeader);
        }
      }

      span.addEvent("streaming_completed");
      span.setStatus(StatusCode.OK);

    } catch (CrypticException | IOException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setStatus(StatusCode.ERROR, "Unexpected runtime error: " + e.getMessage());
      throw e;
    } finally {
      span.end();
    }
  }
}