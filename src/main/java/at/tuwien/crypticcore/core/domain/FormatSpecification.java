package at.tuwien.crypticcore.core.domain;

/**
 * this class represents the format specification of the engine.
 */
public class FormatSpecification {
  private static final byte[] MAGIC = {'C', 'C', 'E'};
  private static final byte VERSION = 0x01;
  private static final int HEADER_LENGTH = MAGIC.length + Byte.BYTES;

  private FormatSpecification() {}

  public static byte[] getMagicBytes() {
    return MAGIC.clone();
  }

  public static byte getVersion() {
    return VERSION;
  }

  public static int getHeaderLength() {
    return HEADER_LENGTH;
  }
}