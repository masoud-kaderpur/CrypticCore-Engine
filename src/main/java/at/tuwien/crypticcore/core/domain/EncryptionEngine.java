package at.tuwien.crypticcore.core.domain;

import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import java.io.IOException;

/**
 * defines the contract of the engine.
 */
public interface EncryptionEngine {

  /**
   * executes the engine.
   *
   * @param context the given context.
   *
   * @throws IOException if there is an exception throughout the process
   * @throws ValidationException if the context is not valid
   */
  void process(Context context) throws IOException, ValidationException;
}