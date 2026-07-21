package at.tuwien.crypticcore.infrastructure.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.tuwien.crypticcore.core.domain.exception.HeaderValidationException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("HeaderHandler Metadata Specification & I/O Tests")
class HeaderHandlerTest {

  @TempDir
  Path tempDir;

  private Path testFile;

  @BeforeEach
  void setUp() {
    testFile = tempDir.resolve("header_test.cce");
  }

  @Nested
  @DisplayName("Header Creation Tests")
  class WriteHeaderTests {

    @Test
    @DisplayName("Should write 4-byte header consisting of 'CCE' magic bytes and version 1")
    void shouldWriteValidHeader() throws IOException {
      try (FileOutputStream out = new FileOutputStream(testFile.toFile())) {
        HeaderHandler.writeHeader(out);
      }

      byte[] fileBytes = Files.readAllBytes(testFile);
      assertThat(fileBytes).hasSize(4);
      assertThat(fileBytes[0]).isEqualTo((byte) 'C');
      assertThat(fileBytes[1]).isEqualTo((byte) 'C');
      assertThat(fileBytes[2]).isEqualTo((byte) 'E');
      assertThat(fileBytes[3]).isEqualTo((byte) 1);
    }
  }

  @Nested
  @DisplayName("Header Verification Tests")
  class CheckHeaderTests {

    @Test
    @DisplayName("Should successfully validate a correctly formatted header")
    void shouldAcceptValidHeader() throws IOException {
      try (FileOutputStream out = new FileOutputStream(testFile.toFile())) {
        HeaderHandler.writeHeader(out);
      }

      try (FileInputStream in = new FileInputStream(testFile.toFile())) {
        assertThatCode(() -> HeaderHandler.checkHeader(in)).doesNotThrowAnyException();
      }
    }

    @Test
    @DisplayName("Should throw HeaderValidationException when file is smaller than 3 magic bytes")
    void shouldThrowOnTruncatedHeader() throws IOException {
      Files.write(testFile, "CC".getBytes(StandardCharsets.US_ASCII));

      try (FileInputStream in = new FileInputStream(testFile.toFile())) {
        assertThatThrownBy(() -> HeaderHandler.checkHeader(in))
            .isInstanceOf(HeaderValidationException.class)
            .hasMessage("Incorrect CrypticCore-Engine Data!");
      }
    }

    @Test
    @DisplayName("Should throw HeaderValidationException when magic bytes do not match 'CCE'")
    void shouldThrowOnInvalidMagicBytes() throws IOException {
      byte[] invalidMagicHeader = new byte[] {'B', 'A', 'D', 1};
      Files.write(testFile, invalidMagicHeader);

      try (FileInputStream in = new FileInputStream(testFile.toFile())) {
        assertThatThrownBy(() -> HeaderHandler.checkHeader(in))
            .isInstanceOf(HeaderValidationException.class)
            .hasMessage("Incorrect CrypticCore-Engine Data!");
      }
    }

    @Test
    @DisplayName("Should throw HeaderValidationException when version byte is incompatible")
    void shouldThrowOnIncompatibleVersion() throws IOException {
      byte[] invalidVersionHeader = new byte[] {'C', 'C', 'E', 2};
      Files.write(testFile, invalidVersionHeader);

      try (FileInputStream in = new FileInputStream(testFile.toFile())) {
        assertThatThrownBy(() -> HeaderHandler.checkHeader(in))
            .isInstanceOf(HeaderValidationException.class)
            .hasMessage("Incompatible version: 2");
      }
    }
  }
}