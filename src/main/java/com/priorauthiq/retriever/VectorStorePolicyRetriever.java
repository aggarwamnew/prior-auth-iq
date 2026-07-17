package com.priorauthiq.retriever;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.PriorAuthRequest;
import com.priorauthiq.rag.PolicyDocumentEtl;
import com.priorauthiq.store.PolicyStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Optional;

/**
 * Retrieval-augmented policy resolution (Slice 3a): the request's clinical
 * context is embedded and searched against the policy corpus in the {@link
 * VectorStore}; the best chunk above the similarity threshold identifies the
 * governing policy via its {@code serviceCode} metadata.
 *
 * <p>The typed {@link CoveragePolicy} is rehydrated from the {@link
 * PolicyStore} rather than parsed back out of chunk text — the vector store
 * answers "which policy applies", the system of record supplies the policy.
 * Works over {@code SimpleVectorStore} today and pgvector in Slice 3b without
 * change, because only the {@link VectorStore} interface is used.
 */
public class VectorStorePolicyRetriever implements PolicyRetriever {

    private final VectorStore vectorStore;
    private final PolicyStore policyStore;
    private final double similarityThreshold;

    public VectorStorePolicyRetriever(VectorStore vectorStore,
                                      PolicyStore policyStore,
                                      double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.policyStore = policyStore;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public Optional<CoveragePolicy> retrieve(PriorAuthRequest request) {
        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(queryText(request))
                .topK(1)
                .similarityThreshold(similarityThreshold)
                .build());

        return hits.stream()
                .findFirst()
                .map(doc -> (String) doc.getMetadata().get(PolicyDocumentEtl.SERVICE_CODE_KEY))
                .flatMap(policyStore::findByServiceCode);
    }

    @Override
    public String strategy() {
        return "vector";
    }

    private String queryText(PriorAuthRequest request) {
        return String.join("\n",
                request.serviceCode(),
                request.diagnosisCode(),
                request.clinicalNotes(),
                String.join("\n", request.documentedHistory()));
    }
}
