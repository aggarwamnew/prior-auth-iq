package com.priorauthiq.config;

import com.priorauthiq.rag.PolicyDocumentEtl;
import com.priorauthiq.retriever.DirectPolicyRetriever;
import com.priorauthiq.retriever.PolicyRetriever;
import com.priorauthiq.retriever.VectorStorePolicyRetriever;
import com.priorauthiq.store.PolicyStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selects the active {@link PolicyRetriever} from {@code priorauthiq.retriever}
 * — the same config-selected-seam pattern as {@link MatcherConfig}.
 *
 * <ul>
 *   <li>{@code direct} (default) — exact service-code lookup, no model, no
 *       keys. The permanent deterministic control.</li>
 *   <li>{@code vector} — RAG over the embedded policy corpus. Requires an
 *       {@link EmbeddingModel} (from a model-provider starter in production;
 *       tests supply a deterministic fake). The corpus is ingested at startup
 *       by {@link PolicyDocumentEtl}.</li>
 * </ul>
 *
 * <p>The {@link VectorStore} here is Spring AI's {@link SimpleVectorStore}
 * (Slice 3a). Slice 3b replaces this single bean with pgvector; nothing else
 * changes, because retriever and ETL depend only on the interface.
 */
@Configuration
public class RetrieverConfig {

    @Bean
    @ConditionalOnProperty(name = "priorauthiq.retriever", havingValue = "direct", matchIfMissing = true)
    PolicyRetriever directPolicyRetriever(PolicyStore policyStore) {
        return new DirectPolicyRetriever(policyStore);
    }

    @Bean
    @ConditionalOnProperty(name = "priorauthiq.retriever", havingValue = "vector")
    VectorStore policyVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean(initMethod = "ingest")
    @ConditionalOnProperty(name = "priorauthiq.retriever", havingValue = "vector")
    PolicyDocumentEtl policyDocumentEtl(PolicyStore policyStore, VectorStore vectorStore) {
        return new PolicyDocumentEtl(policyStore, vectorStore);
    }

    @Bean
    @ConditionalOnProperty(name = "priorauthiq.retriever", havingValue = "vector")
    PolicyRetriever vectorStorePolicyRetriever(
            VectorStore vectorStore,
            PolicyStore policyStore,
            @Value("${priorauthiq.retrieval.similarity-threshold:0.25}") double similarityThreshold) {
        return new VectorStorePolicyRetriever(vectorStore, policyStore, similarityThreshold);
    }
}
