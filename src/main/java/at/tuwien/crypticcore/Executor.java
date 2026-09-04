package at.tuwien.crypticcore;

import at.tuwien.crypticcore.core.domain.CipherAlgorithm;
import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.EncryptionEngine;
import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.Validator;
import at.tuwien.crypticcore.core.engine.XorEncryptionEngine;
import at.tuwien.crypticcore.core.engine.algorithm.XorCipher;
import at.tuwien.crypticcore.infrastructure.io.ContextValidator;
import at.tuwien.crypticcore.infrastructure.io.HeaderHandler;
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

  /**
   * this class is the executer that can be called by the app.
   */
  public static void execute(Context context, Tracer tracer) throws Exception {
    try {
      EncryptionEngine processor = new XorEncryptionEngine(ALGORITHM, VALIDATOR, HANDLER, tracer);
      processor.process(context);

      Files.move(
          context.tempOutputPath(),
          context.outputPath(),
          StandardCopyOption.REPLACE_EXISTING);

    } catch (Exception e) {
      try {
        Files.deleteIfExists(context.tempOutputPath());
      } catch (IOException ex) {
        e.addSuppressed(ex);
      }
      throw e;
    } finally {
      Arrays.fill(context.key(), (byte) 0);
    }
  }
}