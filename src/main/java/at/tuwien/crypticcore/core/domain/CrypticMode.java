package at.tuwien.crypticcore.core.domain;

import java.util.Arrays;

/**
 * Defines the operational state of a {@link StreamProcessor}.
 * <p>This enumeration determines whether the processor should prepend
 * format headers (Encryption) or validate existing headers (Decryption) during the
 * transformation process.</p>
 */
public enum CrypticMode {
  /**
   * Specifies that the input data should be transformed and prefixed with the CrypticCore
   * metadata header.
   */
  ENCRYPTION("encryption", "encrypt"),

  /**
   * Specifies that the input data should be validated against the CrypticCore header before
   * attempting reversal transformation.
   */
  DECRYPTION("decryption", "decrypt");

  private final String[] aliases;

  CrypticMode(String... aliases) {
    this.aliases = aliases;
  }

  /**
   * Parses a string input into a valid {@code CrypticMode}, supporting both standard enum names
   * and common action verbs.
   * <p>Matching is case-insensitive. Supported aliases include "encrypt"
   * for {@code ENCRYPTION} and "decrypt" for {@code DECRYPTION}.</p>
   *
   * @param text the raw string input (e.g., from command line arguments)
   * @return the corresponding {@code CrypticMode}
   * @throws IllegalArgumentException if the text does not match any known mode or alias
   */
  public static CrypticMode fromString(String text) {

    if (text == null) {
      throw new IllegalArgumentException("Mode input text cannot be null");
    }

    String cleanText = text.trim().toLowerCase();

    return Arrays.stream(CrypticMode.values())
        .filter(mode -> mode.name().toLowerCase().equals(cleanText)
            || Arrays.asList(mode.aliases).contains(cleanText))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown mode: " + text));
  }
}