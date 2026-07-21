package at.tuwien.crypticcore.core.domain.exception;

/**
 * Thrown to indicate that pre-flight validation of input parameters or paths has failed.
 */
public class ValidationException extends RuntimeException {

  public ValidationException(String message) {
    super(message);
  }
}