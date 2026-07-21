package at.tuwien.crypticcore.core.domain.exception;

/**
 * Thrown when processed byte counts do not match expected payload sizes.
 */
public class DataTruncationException extends CrypticException {

  public DataTruncationException(String message) {
    super(message);
  }
}