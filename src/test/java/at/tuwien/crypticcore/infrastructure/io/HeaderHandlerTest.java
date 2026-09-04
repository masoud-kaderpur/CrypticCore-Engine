package at.tuwien.crypticcore.infrastructure.io;

import static at.tuwien.crypticcore.core.domain.FormatSpecification.getMagicBytes;
import static at.tuwien.crypticcore.core.domain.FormatSpecification.getVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeaderHandlerTest {

  private HeaderHandler handler;

  @BeforeEach
  void setUp() {
    handler = new HeaderHandler();
  }

  @Test
  @DisplayName("Constructor invocation for 100% class line coverage")
  void testConstructor() {
    assertThat(new HeaderHandler()).isNotNull();
  }

  @Test
  @DisplayName("Happy Path: writeHeader writes magic bytes and version byte in exact order")
  void testWriteHeader() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    handler.writeHeader(out);

    byte[] writtenBytes = out.toByteArray();
    byte[] expectedMagic = getMagicBytes();
    byte expectedVersion = getVersion();

    assertThat(writtenBytes).hasSize(expectedMagic.length + 1);

    byte[] actualMagic = new byte[expectedMagic.length];
    System.arraycopy(writtenBytes, 0, actualMagic, 0, expectedMagic.length);
    assertThat(actualMagic).isEqualTo(expectedMagic);

    assertThat(writtenBytes[writtenBytes.length - 1]).isEqualTo(expectedVersion);
  }

  @Test
  @DisplayName("Happy Path: validateHeader succeeds with matching magic bytes and version")
  void testValidateHeaderSuccess() {
    byte[] validHeader = createHeader(getMagicBytes(), getVersion());
    ByteArrayInputStream in = new ByteArrayInputStream(validHeader);

    assertThatCode(() -> handler.validateHeader(in)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("Branch: Truncated magic bytes (< expected length) triggers first condition of ||")
  void testValidateHeaderTruncatedMagic() {
    byte[] magic = getMagicBytes();
    byte[] truncatedMagic = new byte[Math.max(0, magic.length - 1)];
    System.arraycopy(magic, 0, truncatedMagic, 0, truncatedMagic.length);

    ByteArrayInputStream in = new ByteArrayInputStream(truncatedMagic);

    assertThatThrownBy(() -> handler.validateHeader(in))
        .isInstanceOf(HeaderValidationException.class)
        .hasMessage("incorrect crypticcore-engine header magic bytes!");
  }

  @Test
  @DisplayName("Branch: Corrupted magic bytes with correct length triggers second condition of ||")
  void testValidateHeaderCorruptedMagicBytes() {
    byte[] corruptedMagic = getMagicBytes().clone();
    corruptedMagic[0] ^= 0xFF;

    byte[] payload = createHeader(corruptedMagic, getVersion());
    ByteArrayInputStream in = new ByteArrayInputStream(payload);

    assertThatThrownBy(() -> handler.validateHeader(in))
        .isInstanceOf(HeaderValidationException.class)
        .hasMessage("incorrect crypticcore-engine header magic bytes!");
  }

  @Test
  @DisplayName("Branch: Missing version byte (EOF immediately after magic) triggers fileVersion == -1")
  void testValidateHeaderMissingVersion() {
    ByteArrayInputStream in = new ByteArrayInputStream(getMagicBytes().clone());

    assertThatThrownBy(() -> handler.validateHeader(in))
        .isInstanceOf(HeaderValidationException.class)
        .hasMessage("incompatible or missing format version: -1");
  }

  @Test
  @DisplayName("Branch: Mismatched version byte triggers second condition of version ||")
  void testValidateHeaderIncompatibleVersion() {
    byte wrongVersion = (byte) (getVersion() + 1);
    byte[] payload = createHeader(getMagicBytes(), wrongVersion);
    ByteArrayInputStream in = new ByteArrayInputStream(payload);

    assertThatThrownBy(() -> handler.validateHeader(in))
        .isInstanceOf(HeaderValidationException.class)
        .hasMessageContaining("incompatible or missing format version: " + (wrongVersion & 0xFF));
  }

  private byte[] createHeader(byte[] magic, byte version) {
    byte[] header = new byte[magic.length + 1];
    System.arraycopy(magic, 0, header, 0, magic.length);
    header[header.length - 1] = version;
    return header;
  }
}