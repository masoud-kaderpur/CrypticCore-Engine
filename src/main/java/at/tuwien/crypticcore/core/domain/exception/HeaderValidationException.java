package at.tuwien.crypticcore.core.domain.exception;

/**
 * Thrown when file header validation fails (e.g., invalid magic bytes or unsupported version).
 */
public class HeaderValidationException extends CrypticException {

  public HeaderValidationException(String message) {
    super(message);
  }

  public HeaderValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}