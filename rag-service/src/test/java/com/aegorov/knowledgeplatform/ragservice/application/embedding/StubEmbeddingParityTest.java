package com.aegorov.knowledgeplatform.ragservice.application.embedding;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Guards the invariant that rag-service's stub embedding is identical to the one
 * indexing-service uses to embed stored chunks. If they drift, a query embedding
 * will not line up with chunk embeddings and retrieval silently breaks. The
 * reference algorithm is re-implemented inline here so a change to
 * {@link StubEmbeddingModel} that breaks compatibility fails the test.
 */
class StubEmbeddingParityTest {

    private static final int DIMENSIONS = 768;

    @Test
    void matchesIndexingServiceAlgorithm() {
        StubEmbeddingModel model = new StubEmbeddingModel(DIMENSIONS);

        for (String text : new String[] {"", "hello world", "Interview questions about Java concurrency"}) {
            float[] actual = model.embed(text);
            float[] expected = referenceEmbed(text);
            assertThat(actual).containsExactly(expected);
        }
    }

    @Test
    void isDeterministicNormalizedAndCorrectlySized() {
        StubEmbeddingModel model = new StubEmbeddingModel(DIMENSIONS);

        float[] first = model.embed("stable input");
        float[] second = model.embed("stable input");

        assertThat(first).hasSize(DIMENSIONS).containsExactly(second);

        double sumOfSquares = 0.0;
        for (float value : first) {
            sumOfSquares += (double) value * value;
        }
        assertThat(Math.sqrt(sumOfSquares)).isCloseTo(1.0, within(1e-4));
    }

    private static float[] referenceEmbed(String text) {
        Random random = new Random(seedFor(text == null ? "" : text));
        float[] vector = new float[DIMENSIONS];
        double sumOfSquares = 0.0;
        for (int i = 0; i < DIMENSIONS; i++) {
            float value = (float) random.nextGaussian();
            vector[i] = value;
            sumOfSquares += (double) value * value;
        }
        float norm = (float) Math.sqrt(sumOfSquares);
        if (norm > 0.0f) {
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    private static long seedFor(String text) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xffL);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
