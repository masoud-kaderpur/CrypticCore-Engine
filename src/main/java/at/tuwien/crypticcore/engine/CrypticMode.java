package at.tuwien.crypticcore.engine;

/**
 * Defines the operational state of the {@link EncryptionEngine}.
 * <p>This enumeration determines whether the engine should prepend
 * format headers (Encryption) or validate existing headers (Decryption)
 * during the transformation process.</p>
 */
public enum CrypticMode {
  /**
   * Specifies that the input data should be transformed and
   * prefixed with the CrypticCore metadata header.
   */
  ENCRYPTION,
  /**
   * Specifies that the input data should be validated against
   * the CrypticCore header before attempting reversal transformation.
   */
  DECRYPTION;

  /**
   * Parses a string input into a valid {@code CrypticMode}, supporting
   * both standard enum names and common action verbs.
   * <p>Matching is case-insensitive. Supported aliases include "encrypt"
   * for {@code ENCRYPTION} and "decrypt" for {@code DECRYPTION}.</p>
   * * @param text the raw string input (e.g., from command line arguments)
   *
   * @return the corresponding {@code CrypticMode}
   * @throws IllegalArgumentException if the text does not match any known mode or alias
   */
  public static CrypticMode fromString(String text) {
    for (CrypticMode mode : CrypticMode.values()) {
      if (mode.name().equalsIgnoreCase(text) ||
              (mode == ENCRYPTION && text.equalsIgnoreCase("encrypt")) ||
              (mode == DECRYPTION && text.equalsIgnoreCase("decrypt"))) {
        return mode;
      }
    }
    throw new IllegalArgumentException("Unknown mode: " + text);
  }
}
