package at.tuwien.crypticcore.core.domain;

import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * this interface is responsible en- or decoding of the header.
 */
public interface HeaderCodec {

  /**
   * this method is responsible for writing the header.
   *
   * @param out the output, where to write to.
   *
   * @throws IOException if there is an exception during the streaming process.
   */
  void writeHeader(OutputStream out) throws IOException;

  /**
   * this method is responsible for validating the header.
   *
   * @param in the input, where to read from.
   *
   * @throws IOException if there is an exception during the streaming process.
   *
   * @throws HeaderValidationException if the header is not valid
   */
  void validateHeader(InputStream in) throws IOException, HeaderValidationException;
}
