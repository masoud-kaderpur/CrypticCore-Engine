package at.tuwien.crypticcore.infrastructure.io;

import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Handles the CrypticCore-specific file format metadata.
 * <p>This class is responsible for the specification and persistence of the
 * file header, ensuring version compatibility and format integrity.</p>
 */
public class HeaderHandler {

  private static final byte[] MAGIC = "CCE".getBytes(StandardCharsets.US_ASCII);
  private static final byte VERSION = 1;

  private HeaderHandler() {
    // Utility class
  }

  /**
   * Persists the CrypticCore metadata header to the output stream.
   *
   * @param out the output stream to write the header to
   * @throws IOException if the write operation fails
   */
  public static void writeHeader(FileOutputStream out) throws IOException {
    out.write(MAGIC);
    out.write(VERSION);
  }

  /**
   * Validates the structural integrity of the input file by verifying the header.
   *
   * @param in the input stream positioned at the start of the file
   * @throws HeaderValidationException if the header is corrupt or version is incompatible
   * @throws IOException            if reading from the stream fails
   */
  public static void checkHeader(FileInputStream in) throws IOException {
    byte[] fileMagic = new byte[MAGIC.length];

    if (in.read(fileMagic) != MAGIC.length || !Arrays.equals(fileMagic, MAGIC)) {
      throw new HeaderValidationException("Incorrect CrypticCore-Engine Data!");
    }

    int fileVersion = in.read();
    if (fileVersion != VERSION) {
      throw new HeaderValidationException("Incompatible version: " + fileVersion);
    }
  }
}