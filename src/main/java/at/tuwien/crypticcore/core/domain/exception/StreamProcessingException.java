package at.tuwien.crypticcore.core.domain.exception;

/**
 * Thrown when an unrecoverable failure occurs during streaming transformation.
 */
public class StreamProcessingException extends CrypticException {

  public StreamProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}