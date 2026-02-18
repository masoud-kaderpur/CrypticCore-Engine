package at.tuwien.crypticcore.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Handles the CrypticCore-specific file format metadata.
 * <p>This class is responsible for the specification and persistence of the
 * file header, ensuring version compatibility and format integrity.</p>
 */
public class HeaderHandler {

  private static final byte[] MAGIC = "CCE".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
  private static final byte VERSION = 1;

  /**
   * Persists the CrypticCore metadata header to the output stream.
   * <p>This ensures that any future decryption attempts can identify the file
   * type and ensure version compatibility before processing data.</p> * @param out the output
   * stream to write the header to
   *
   * @throws IOException if the write operation fails
   */
  public static void writeHeader(FileOutputStream out) throws IOException {
    out.write(MAGIC);
    out.write(VERSION);
  }

  /**
   * Validates the structural integrity of the input file by verifying the header.
   * <p>The header must consist of the "CCE" magic bytes followed by a single-byte version
   * identifier.</p> * @param in the input stream positioned at the start of the file*
   *
   * @throws IOException if the magic bytes do not match or the version is incompatible
   */
  public static void checkHeader(FileInputStream in) throws IOException {
    byte[] fileMagic = new byte[3];
    if (in.read(fileMagic) != 3 || !Arrays.equals(fileMagic, MAGIC)) {
      throw new IOException("Incorrect CrypticCore-Engine Data!");
    }
    int fileVersion = in.read();
    if (fileVersion != VERSION) {
      throw new IOException("Incompatible version: " + fileVersion);
    }
  }
}