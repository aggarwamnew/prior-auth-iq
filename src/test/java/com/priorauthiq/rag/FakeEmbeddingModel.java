package com.priorauthiq.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic bag-of-words embedding for tests: each token hashes to one of
 * {@value #DIMENSIONS} buckets and the vector is L2-normalised, so cosine
 * similarity reflects real token overlap. No network, no keys, fully
 * repeatable — the embedding-side equivalent of the mocked ChatModel used in
 * the Slice 2 tests.
 */
public class FakeEmbeddingModel implements EmbeddingModel {

    static final int DIMENSIONS = 256;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> texts = request.getInstructions();
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(new Embedding(embed(texts.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String token : text.toLowerCase(Locale.ROOT).split("\\W+")) {
            if (!token.isBlank()) {
                vector[Math.floorMod(token.hashCode(), DIMENSIONS)] += 1.0f;
            }
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }
        return vector;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
