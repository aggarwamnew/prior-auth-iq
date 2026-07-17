package com.priorauthiq.retriever;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.PolicyCriterion;
import com.priorauthiq.model.PriorAuthRequest;
import com.priorauthiq.rag.FakeEmbeddingModel;
import com.priorauthiq.rag.PolicyDocumentEtl;
import com.priorauthiq.store.PolicyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the vector retrieval path: policies embedded by the ETL,
 * retrieved by clinical similarity over the deterministic
 * {@link FakeEmbeddingModel} — no network, no keys.
 */
class VectorStorePolicyRetrieverTest {

    private PolicyStore policyStore;
    private VectorStorePolicyRetriever retriever;

    @BeforeEach
    void setUp() {
        policyStore = new PolicyStore();
        policyStore.save(new CoveragePolicy(
                "J0135",
                "Adalimumab (Humira)",
                "Moderate-to-severe rheumatoid arthritis",
                List.of(
                        new PolicyCriterion("RA-DIAGNOSIS",
                                "Documented moderate-to-severe rheumatoid arthritis",
                                List.of("rheumatoid arthritis")),
                        new PolicyCriterion("RA-STEP-THERAPY",
                                "Trial and failure of methotrexate for at least 3 months",
                                List.of("methotrexate")))));
        policyStore.save(new CoveragePolicy(
                "72148",
                "MRI lumbar spine without contrast",
                "Low back pain with suspected radiculopathy",
                List.of(
                        new PolicyCriterion("MRI-CONSERVATIVE",
                                "At least 6 weeks of conservative treatment (physical therapy or NSAIDs)",
                                List.of("physical therapy")),
                        new PolicyCriterion("MRI-RED-FLAG-OR-DURATION",
                                "Neurological deficit or symptoms persisting beyond 6 weeks",
                                List.of("radiculopathy")))));

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(new FakeEmbeddingModel()).build();
        new PolicyDocumentEtl(policyStore, vectorStore).ingest();
        retriever = new VectorStorePolicyRetriever(vectorStore, policyStore, 0.25);
    }

    @Test
    void retrievesImagingPolicyFromClinicalContext() {
        Optional<CoveragePolicy> policy = retriever.retrieve(request(
                "72148", "M54.16",
                "Low back pain with radiculopathy after 8 weeks of physical therapy",
                List.of("Conservative treatment documented", "MRI lumbar spine requested")));

        assertThat(policy).isPresent();
        assertThat(policy.get().serviceCode()).isEqualTo("72148");
    }

    @Test
    void retrievesBiologicPolicyFromClinicalContext() {
        Optional<CoveragePolicy> policy = retriever.retrieve(request(
                "J0135", "M06.9",
                "Moderate-to-severe rheumatoid arthritis, failed methotrexate",
                List.of("Adalimumab requested", "Methotrexate trial 4 months")));

        assertThat(policy).isPresent();
        assertThat(policy.get().serviceCode()).isEqualTo("J0135");
    }

    @Test
    void returnsEmptyWhenNothingIsSimilarEnough() {
        Optional<CoveragePolicy> policy = retriever.retrieve(request(
                "99999", "Z00.0",
                "Entirely unrelated wellness visit paperwork",
                List.of("Nothing clinically relevant here")));

        assertThat(policy).isEmpty();
    }

    private PriorAuthRequest request(String serviceCode, String diagnosisCode,
                                     String notes, List<String> history) {
        return new PriorAuthRequest("req-test", "patient-1", serviceCode,
                diagnosisCode, notes, history, Instant.now());
    }
}
