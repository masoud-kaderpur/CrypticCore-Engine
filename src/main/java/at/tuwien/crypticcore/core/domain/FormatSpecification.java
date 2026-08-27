package at.tuwien.crypticcore.core.domain;

/**
 * this class represents the format specification of the engine.
 */
public class FormatSpecification {
  private FormatSpecification() {}

  public static final byte[] MAGIC = {'C', 'C', 'E'};
  public static final byte VERSION = 0x01;
  public static final int HEADER_LENGTH = MAGIC.length + Byte.BYTES;
}
