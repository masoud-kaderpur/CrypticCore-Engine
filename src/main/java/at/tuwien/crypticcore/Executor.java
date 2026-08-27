package at.tuwien.crypticcore;

import static at.tuwien.crypticcore.core.domain.FormatSpecification.HEADER_LENGTH;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.EncryptionEngine;
import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.Validator;
import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import at.tuwien.crypticcore.core.engine.XorEncryptionEngine;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import at.tuwien.crypticcore.infrastructure.io.ContextValidator;
import at.tuwien.crypticcore.infrastructure.io.HeaderHandler;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * this class is the executer that can be called by the app.
 */
public class Executor {

  private static final CipherAlgorithm ALGORITHM = new XorCipher();
  private static final Validator VALIDATOR = new ContextValidator();
  private static final HeaderCodec HANDLER = new HeaderHandler();
  private static final Tracer TRACER = GlobalOpenTelemetry.getTracer(
      "at.tuwien.crypticcore.core.engine", Executor.class.getPackage().getImplementationVersion());

  /**
   * this method executes the engine.
   *
   * @param context the given context.
   */
  public static void execute(Context context) throws IOException {
    try {

      if (context.mode() == CrypticMode.DECRYPTION && context.fileSize() < HEADER_LENGTH) {
        throw new HeaderValidationException("the file is too small for crypticcore header.");
      }

      EncryptionEngine processor = new XorEncryptionEngine(ALGORITHM, VALIDATOR, HANDLER, TRACER);

      processor.process(context);

      Files.move(
          context.tempOutputPath(),
          context.outputPath(),
          StandardCopyOption.REPLACE_EXISTING);

    } catch (Exception e) {
      Files.deleteIfExists(context.tempOutputPath());
    } finally {
      Arrays.fill(context.key(), (byte) 0);
    }
  }
}
