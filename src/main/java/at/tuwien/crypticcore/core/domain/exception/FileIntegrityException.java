package at.tuwien.crypticcore.core.domain.exception;

/**
 * Thrown when file integrity checks or I/O pre-conditions fail.
 */
public class FileIntegrityException extends CrypticException {

  public FileIntegrityException(String message) {
    super(message);
  }

  public FileIntegrityException(String message, Throwable cause) {
    super(message, cause);
  }
}