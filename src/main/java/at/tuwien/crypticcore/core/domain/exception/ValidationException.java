package at.tuwien.crypticcore.core.domain.exception;

/**
 * thrown to indicate that validation of input parameters or paths has failed.
 */
public class ValidationException extends CrypticException {

  public ValidationException(String message) {
    super(message);
  }
}