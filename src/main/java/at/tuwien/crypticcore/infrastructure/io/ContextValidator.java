package at.tuwien.crypticcore.infrastructure.io;


import at.tuwien.crypticcore.core.domain.Context;
import at.tuwien.crypticcore.core.domain.Validator;
import at.tuwien.crypticcore.core.domain.exception.ValidationException;
import java.io.IOException;
import java.nio.file.Files;

/**
 * this class implements the interface validator and is responsible for validating the context.
 */
public class ContextValidator implements Validator {

  @Override
  public void validate(Context context) throws ValidationException, IOException {
    if (context.mode() == null) {
      throw new ValidationException("mode must not be null");
    }

    if (!Files.exists(context.inputPath())) {
      throw new ValidationException("input path does not exist: " + context.inputPath());
    }

    if (Files.exists(context.outputPath())
        && Files.isSameFile(context.inputPath(), context.outputPath())) {
      throw new ValidationException("input and output paths must not be the same!");
    }

    if (context.key() == null || context.key().length == 0) {
      throw new ValidationException("Key must not be null or empty");
    }

    if (context.fileSize() <= 0) {
      throw new ValidationException("File size must be greater than 0");
    }
  }
}