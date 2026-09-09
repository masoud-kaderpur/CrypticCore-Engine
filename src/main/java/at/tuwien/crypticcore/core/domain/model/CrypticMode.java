package at.tuwien.crypticcore.core.domain.model;

import java.util.Arrays;

/**
 * this enum defines the state of a process.
 */
public enum CrypticMode {

  ENCRYPTION("encryption", "encrypt"),
  DECRYPTION("decryption", "decrypt");

  private final String[] aliases;

  CrypticMode(String... aliases) {
    this.aliases = aliases;
  }

  /**
   * this method parses a string input into a valid {@code CrypticMode}.
   *
   * @param text the raw string input.
   * @return the {@code CrypticMode}
   * @throws IllegalArgumentException if the text does not match any known mode or alias
   */
  public static CrypticMode fromString(String text) {

    if (text == null) {
      throw new IllegalArgumentException("mode input text cannot be null");
    }

    String cleanText = text.trim().toLowerCase();

    return Arrays.stream(CrypticMode.values())
        .filter(mode -> mode.name().toLowerCase().equals(cleanText)
            || Arrays.asList(mode.aliases).contains(cleanText))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown mode: " + text));
  }
}