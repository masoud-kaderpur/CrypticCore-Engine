package at.tuwien.crypticcore.core.engine;

import static at.tuwien.crypticcore.core.domain.FormatSpecification.getHeaderLength;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.EncryptionEngine;
import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.Validator;
import at.tuwien.crypticcore.core.domain.exception.CrypticException;
import at.tuwien.crypticcore.core.domain.exception.DataTruncationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * this class represents the core xor engine implementation.
 */
public class XorEncryptionEngine implements EncryptionEngine {

  private static final int BUFFER_SIZE = 8192;
  private final CipherAlgorithm algorithm;
  private final Validator validator;
  private final HeaderCodec headerCodec;
  private final Tracer tracer;

  /**
   * this constructor generates the xor encryption engine.
   *
   * @param algorithm the cipher algorithm (e.g. XOR)
   * @param validator the validator (e.g. ContextValidator)
   * @param headerCodec the headercodec (e.g. HeaderHandler)
   */
  public XorEncryptionEngine(
      CipherAlgorithm algorithm,
      Validator validator,
      HeaderCodec headerCodec,
      Tracer tracer) {
    this.algorithm = algorithm;
    this.validator = validator;
    this.headerCodec = headerCodec;
    this.tracer = tracer;
  }

  @Override
  public void process(Context context) throws IOException {

    Path fileName = context.inputPath().getFileName();

    Span span = tracer.spanBuilder(context.mode().name().toLowerCase() + "_file")
        .setAttribute("file.size", context.fileSize())
        .setAttribute("file.name", (fileName != null) ? fileName.toString() : "root")
        .setAttribute("algorithm.name", algorithm.getName())
        .startSpan();

    span.addEvent("engine_started");

    try {
      int bytesRead;
      long totalBytesProcessed = 0;
      byte[] buffer = new byte[BUFFER_SIZE];

      validator.validate(context);

      span.addEvent("inputs_verified");

      try (FileInputStream in = new FileInputStream(context.inputPath().toFile());
          FileOutputStream out = new FileOutputStream(context.tempOutputPath().toFile())) {

        if (context.mode() == CrypticMode.ENCRYPTION) {
          headerCodec.writeHeader(out);
          span.addEvent("header_written");
        } else {
          headerCodec.validateHeader(in);
          span.addEvent("header_validated");
        }

        while ((bytesRead = in.read(buffer)) != -1) {
          algorithm.transform(buffer, bytesRead, context.key(), totalBytesProcessed);
          out.write(buffer, 0, bytesRead);
          totalBytesProcessed += bytesRead;
        }
      }

      if (context.mode() == CrypticMode.ENCRYPTION) {
        if (totalBytesProcessed != context.fileSize()) {
          throw new DataTruncationException("Data truncation during encryption! Expected: "
              + context.fileSize() + " bytes, processed: " + totalBytesProcessed);
        }
      } else {
        long totalReadWithHeader = totalBytesProcessed + getHeaderLength();
        if (totalReadWithHeader != context.fileSize()) {
          throw new DataTruncationException("Data truncation during decryption! Expected: "
              + context.fileSize() + " bytes, accounted: " + totalReadWithHeader);
        }
      }

      span.addEvent("engine_closed");
      span.setStatus(StatusCode.OK);

    } catch (CrypticException | IOException e) {
      span.recordException(e);
      span.setAttribute("error.type", e.getClass().getName());
      span.setStatus(StatusCode.ERROR, "processing failed: " + e.getClass().getSimpleName());
      throw e;
    } catch (RuntimeException e) {
      span.recordException(e);
      span.setAttribute("error.type", e.getClass().getName());
      span.setStatus(StatusCode.ERROR, "unexpected runtime error");
      throw e;
    } finally {
      span.end();
    }
  }
}