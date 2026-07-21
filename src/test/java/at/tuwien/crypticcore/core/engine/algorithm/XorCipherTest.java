package at.tuwien.crypticcore.core.engine.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("XorCipher Algorithm Core Tests")
class XorCipherTest {

  private XorCipher cipher;

  @BeforeEach
  void setUp() {
    this.cipher = new XorCipher();
  }

  @Test
  @DisplayName("Should return correct algorithm name")
  void shouldReturnCorrectName() {
    assertThat(cipher.getName()).isEqualTo("XOR");
  }

  @Nested
  @DisplayName("Transformation & Involution Tests")
  class TransformationTests {

    @Test
    @DisplayName("Should transform data and correctly restore it on second pass (Involution Property)")
    void shouldBeInvolutive() {
      byte[] original = "High Performance Streaming XOR Engine 2026".getBytes(StandardCharsets.UTF_8);
      byte[] buffer = original.clone();
      byte[] key = "SecretKey123".getBytes(StandardCharsets.UTF_8);

      cipher.transform(buffer, buffer.length, key, 0);

      assertThat(buffer).isNotEqualTo(original);

      cipher.transform(buffer, buffer.length, key, 0);

      assertThat(buffer).isEqualTo(original);
    }

    @Test
    @DisplayName("Should respect streamOffset across multiple streaming chunks")
    void shouldRespectStreamOffsetForChunkedStreams() {
      byte[] fullPayload = "StreamingChunkingValidationTestPayload".getBytes(StandardCharsets.UTF_8);
      byte[] key = "Key123".getBytes(StandardCharsets.UTF_8);

      int chunk1Length = 10;
      int chunk2Length = fullPayload.length - chunk1Length;

      byte[] chunk1 = new byte[chunk1Length];
      byte[] chunk2 = new byte[chunk2Length];
      System.arraycopy(fullPayload, 0, chunk1, 0, chunk1Length);
      System.arraycopy(fullPayload, chunk1Length, chunk2, 0, chunk2Length);

      cipher.transform(chunk1, chunk1Length, key, 0);

      cipher.transform(chunk2, chunk2Length, key, chunk1Length);

      byte[] singlePassBuffer = fullPayload.clone();
      cipher.transform(singlePassBuffer, singlePassBuffer.length, key, 0);

      byte[] combinedResult = new byte[fullPayload.length];
      System.arraycopy(chunk1, 0, combinedResult, 0, chunk1Length);
      System.arraycopy(chunk2, 0, combinedResult, chunk1Length, chunk2Length);

      assertThat(combinedResult).isEqualTo(singlePassBuffer);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 128, 8192, 16384})
    @DisplayName("Should correctly transform varying buffer sizes")
    void shouldHandleVaryingBufferSizes(int size) {
      byte[] original = new byte[size];
      for (int i = 0; i < size; i++) {
        original[i] = (byte) (i % 256);
      }
      byte[] buffer = original.clone();
      byte[] key = "DynamicKey2026".getBytes(StandardCharsets.UTF_8);

      cipher.transform(buffer, buffer.length, key, 0);
      assertThat(buffer).isNotEqualTo(original);

      cipher.transform(buffer, buffer.length, key, 0);
      assertThat(buffer).isEqualTo(original);
    }
  }

  @Nested
  @DisplayName("Static Memory Sanitation Tests")
  class MemorySanitationTests {

    @Test
    @DisplayName("Should overwrite key array with zero-bytes using static wipeMemory")
    void shouldZeroOutKeyMemory() {
      byte[] key = "SensitivePassword123!".getBytes(StandardCharsets.UTF_8);
      XorCipher.wipeMemory(key);
      assertThat(key).containsOnly((byte) 0);
    }

    @Test
    @DisplayName("Should handle null key gracefully in wipeMemory without throwing NPE")
    void shouldHandleNullInWipeMemory() {
      XorCipher.wipeMemory(null);
    }
  }
}