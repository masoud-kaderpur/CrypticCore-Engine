//package at.tuwien.crypticcore.core.domain;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import at.tuwien.crypticcore.core.domain.model.CrypticMode;
//import org.junit.jupiter.api.Test;
//
//class CrypticModeTest {
//
//  @Test
//  void fromString_ShouldMatchExactEnumNamesCaseInsensitively() {
//    assertEquals(CrypticMode.ENCRYPTION, CrypticMode.fromString("ENCRYPTION"));
//    assertEquals(CrypticMode.DECRYPTION, CrypticMode.fromString("decryption"));
//  }
//
//  @Test
//  void fromString_ShouldMatchConfiguredAliases() {
//    assertEquals(CrypticMode.ENCRYPTION, CrypticMode.fromString("encrypt"));
//    assertEquals(CrypticMode.DECRYPTION, CrypticMode.fromString("decrypt"));
//  }
//
//  @Test
//  void fromString_ShouldHandleSurroundingWhitespace() {
//    assertEquals(CrypticMode.ENCRYPTION, CrypticMode.fromString("  ENCRYPT  "));
//    assertEquals(CrypticMode.DECRYPTION, CrypticMode.fromString(" \tdecrypt\n "));
//  }
//
//  @Test
//  void fromString_ShouldThrowException_WhenInputIsNull() {
//    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//      CrypticMode.fromString(null);
//    });
//    assertEquals("Mode input text cannot be null", exception.getMessage());
//  }
//
//  @Test
//  void fromString_ShouldThrowException_WhenInputIsUnknown() {
//    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//      CrypticMode.fromString("obfuscate");
//    });
//    assertEquals("Unknown mode: obfuscate", exception.getMessage());
//  }
//}