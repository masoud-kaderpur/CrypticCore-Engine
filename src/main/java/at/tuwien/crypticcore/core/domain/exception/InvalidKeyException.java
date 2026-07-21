package at.tuwien.crypticcore.core.domain.exception;

/**
 * Thrown when an invalid or insecure encryption key is provided.
 */
public class InvalidKeyException extends CrypticException {

  public InvalidKeyException(String message) {
    super(message);
  }
}