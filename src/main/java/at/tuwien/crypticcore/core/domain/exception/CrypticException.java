package at.tuwien.crypticcore.core.domain.exception;

/**
 * Abstract root exception for all CrypticCore domain and engine failures.
 */
public abstract class CrypticException extends RuntimeException {

  protected CrypticException(String message) {
    super(message);
  }

  protected CrypticException(String message, Throwable cause) {
    super(message, cause);
  }
}