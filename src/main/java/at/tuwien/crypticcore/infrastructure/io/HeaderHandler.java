package at.tuwien.crypticcore.infrastructure.io;

import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * handles the cce specific file format metadata.
 */
public class HeaderHandler implements HeaderCodec {

  private static final byte[] MAGIC = "CCE".getBytes(StandardCharsets.US_ASCII);
  private static final byte VERSION = 1;

  @Override
  public void writeHeader(OutputStream out) throws IOException {
    out.write(MAGIC);
    out.write(VERSION);
  }

  @Override
  public void validateHeader(InputStream in) throws IOException, HeaderValidationException {
    byte[] fileMagic = new byte[MAGIC.length];

    if (in.read(fileMagic) != MAGIC.length || !Arrays.equals(fileMagic, MAGIC)) {
      throw new HeaderValidationException("incorrect crypticCore-engine data!");
    }

    int fileVersion = in.read();
    if (fileVersion != VERSION) {
      throw new HeaderValidationException("incompatible version: " + fileVersion);
    }
  }
}