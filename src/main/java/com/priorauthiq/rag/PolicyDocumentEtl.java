package com.priorauthiq.rag;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.PolicyCriterion;
import com.priorauthiq.store.PolicyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ETL for the policy corpus (Slice 3a): render each coverage policy as a
 * document, split into chunks, embed into the {@link VectorStore}.
 *
 * <p>Documents carry the policy's {@code serviceCode} as metadata; the
 * splitter propagates metadata to every chunk, so any retrieved chunk can be
 * mapped back to its governing policy. The pipeline is the standard Spring AI
 * shape — reader → splitter → vector store — and is storage-agnostic: Slice
 * 3b points the same ETL at pgvector.
 */
public class PolicyDocumentEtl {

    /** Metadata key linking every chunk back to its policy. */
    public static final String SERVICE_CODE_KEY = "serviceCode";

    private static final Logger log = LoggerFactory.getLogger(PolicyDocumentEtl.class);

    private final PolicyStore policyStore;
    private final VectorStore vectorStore;

    public PolicyDocumentEtl(PolicyStore policyStore, VectorStore vectorStore) {
        this.policyStore = policyStore;
        this.vectorStore = vectorStore;
    }

    /** Load the current policy corpus into the vector store. */
    public void ingest() {
        List<Document> documents = policyStore.findAll().stream()
                .map(this::toDocument)
                .toList();

        List<Document> chunks = new TokenTextSplitter().apply(documents);
        vectorStore.add(chunks);

        log.info("Policy ETL: embedded {} policies as {} chunks", documents.size(), chunks.size());
    }

    private Document toDocument(CoveragePolicy policy) {
        String text = """
                Coverage policy: %s (service code %s)
                Indication: %s
                Criteria:
                %s""".formatted(
                policy.serviceName(),
                policy.serviceCode(),
                policy.indication(),
                policy.criteria().stream()
                        .map(PolicyCriterion::description)
                        .map(d -> "- " + d)
                        .collect(Collectors.joining("\n")));

        return new Document(text, Map.of(SERVICE_CODE_KEY, policy.serviceCode()));
    }
}
