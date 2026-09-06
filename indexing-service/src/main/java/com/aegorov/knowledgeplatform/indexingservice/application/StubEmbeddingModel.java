package com.aegorov.knowledgeplatform.indexingservice.application;

import com.aegorov.knowledgeplatform.indexingservice.config.EmbeddingProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/**
 * Deterministic, offline embedding model used when no real provider is
 * configured ({@code knowledge-platform.embedding.provider=stub}). Produces an
 * L2-normalized pseudo-random vector seeded from the text content, so identical
 * text always yields the same embedding (stable across JVMs and runs). Suitable
 * for local development, tests and cosine similarity demos without any API keys
 * or a running model server.
 */
public class StubEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public StubEmbeddingModel(EmbeddingProperties embeddingProperties) {
        this.dimensions = embeddingProperties.dimensions();
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
    public List<float[]> embed(List<String> texts) {
        return texts.stream().map(this::embed).toList();
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
