package at.tuwien.crypticcore.core.domain;

import at.tuwien.crypticcore.core.domain.model.CrypticMode;
import java.nio.file.Path;

/**
 * the context of the engine.
 *
 * @param mode the cryptic mode
 * @param inputPath the input path
 * @param outputPath the output path
 * @param key the secret key
 * @param fileSize the size of the file
 */
public record Context(
    CrypticMode mode,
    Path inputPath,
    Path outputPath,
    byte[] key,
    long fileSize) {
}
