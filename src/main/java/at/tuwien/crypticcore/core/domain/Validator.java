package at.tuwien.crypticcore.core.domain;

import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import java.io.IOException;

/**
 * this interface is responsible for validating the context of the engine.
 */
public interface Validator {

  /**
   * this method checks if the given context is valid.
   *
   * @param context the context you want to validate
   *
   * @throws ValidationException if the given context is not valid
   */
  void validate(Context context) throws ValidationException, IOException;
}
