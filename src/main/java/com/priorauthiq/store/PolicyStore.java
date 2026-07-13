package com.priorauthiq.store;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.PolicyCriterion;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory coverage-policy corpus, keyed by service code.
 *
 * <p>Slice 1 seeds a small set of realistic (synthetic) policies. In Slice 3
 * this corpus is backed by a vector store and retrieved via RAG, so the matcher
 * can ground against a large policy library rather than an in-memory map.
 */
@Repository
public class PolicyStore {

    private final ConcurrentMap<String, CoveragePolicy> byServiceCode = new ConcurrentHashMap<>();

    public Optional<CoveragePolicy> findByServiceCode(String serviceCode) {
        return Optional.ofNullable(byServiceCode.get(serviceCode));
    }

    public List<CoveragePolicy> findAll() {
        return List.copyOf(byServiceCode.values());
    }

    public void save(CoveragePolicy policy) {
        byServiceCode.put(policy.serviceCode(), policy);
    }

    @PostConstruct
    void seed() {
        // Adalimumab (Humira) for rheumatoid arthritis — a classic step-therapy PA.
        save(new CoveragePolicy(
                "J0135",
                "Adalimumab (Humira)",
                "Moderate-to-severe rheumatoid arthritis",
                List.of(
                        new PolicyCriterion(
                                "RA-DIAGNOSIS",
                                "Documented moderate-to-severe rheumatoid arthritis",
                                List.of("rheumatoid arthritis", "moderate-to-severe ra", "severe ra")),
                        new PolicyCriterion(
                                "RA-STEP-THERAPY",
                                "Trial and failure of methotrexate for at least 3 months",
                                List.of("methotrexate")),
                        new PolicyCriterion(
                                "RA-TB-SCREEN",
                                "Tuberculosis screening on file prior to biologic therapy",
                                List.of("tb screening", "tuberculosis screening", "quantiferon"))
                )
        ));

        // MRI lumbar spine — imaging PA with conservative-treatment step therapy.
        save(new CoveragePolicy(
                "72148",
                "MRI lumbar spine without contrast",
                "Low back pain with suspected radiculopathy",
                List.of(
                        new PolicyCriterion(
                                "MRI-CONSERVATIVE",
                                "At least 6 weeks of conservative treatment (physical therapy or NSAIDs)",
                                List.of("physical therapy", "conservative treatment", "nsaid")),
                        new PolicyCriterion(
                                "MRI-RED-FLAG-OR-DURATION",
                                "Neurological deficit or symptoms persisting beyond 6 weeks",
                                List.of("radiculopathy", "neurological deficit", "persistent", "6 weeks"))
                )
        ));
    }
}
