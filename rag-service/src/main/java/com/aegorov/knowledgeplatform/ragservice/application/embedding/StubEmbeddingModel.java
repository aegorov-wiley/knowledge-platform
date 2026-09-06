package com.aegorov.knowledgeplatform.ragservice.application.embedding;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Deterministic, offline embedding model used when no real provider is
 * configured ({@code knowledge-platform.embedding.provider=stub}).
 * <p>
 * This algorithm is intentionally IDENTICAL to indexing-service's
 * {@code StubEmbeddingModel} (same FNV-1a seed, same {@link Random#nextGaussian()}
 * sequence, same L2 normalization). If the two ever diverge, a query embedding
 * produced here will not line up with the chunk embeddings stored by
 * indexing-service and retrieval becomes meaningless.
 * {@code StubEmbeddingParityTest} guards the invariant.
 */
public class StubEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public StubEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        Random random = new Random(seedFor(text == null ? "" : text));
        float[] vector = new float[dimensions];
        double sumOfSquares = 0.0;
        for (int i = 0; i < dimensions; i++) {
            float value = (float) random.nextGaussian();
            vector[i] = value;
            sumOfSquares += (double) value * value;
        }
        float norm = (float) Math.sqrt(sumOfSquares);
        if (norm > 0.0f) {
            for (int i = 0; i < dimensions; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private long seedFor(String text) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xffL);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
