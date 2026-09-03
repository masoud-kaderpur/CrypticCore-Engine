package at.tuwien.crypticcore.infrastructure.io;

import static at.tuwien.crypticcore.core.domain.FormatSpecification.getMagicBytes;
import static at.tuwien.crypticcore.core.domain.FormatSpecification.getVersion;

import at.tuwien.crypticcore.core.domain.HeaderCodec;
import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * handles the cce specific file format metadata.
 */
public class HeaderHandler implements HeaderCodec {

  @Override
  public void writeHeader(OutputStream out) throws IOException {
    out.write(getMagicBytes());
    out.write(getVersion());
  }

  @Override
  public void validateHeader(InputStream in) throws IOException, HeaderValidationException {
    byte[] expectedMagic = getMagicBytes();
    byte[] fileMagic = in.readNBytes(expectedMagic.length);

    if (fileMagic.length != expectedMagic.length || !Arrays.equals(fileMagic, expectedMagic)) {
      throw new HeaderValidationException("incorrect crypticcore-engine header magic bytes!");
    }

    int fileVersion = in.read();
    if (fileVersion == -1 || (byte) fileVersion != getVersion()) {
      throw new HeaderValidationException("incompatible or missing format version: " + fileVersion);
    }
  }
}