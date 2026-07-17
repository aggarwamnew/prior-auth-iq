package com.priorauthiq.service;

import com.priorauthiq.matcher.PolicyMatcher;
import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PriorAuthRequest;
import com.priorauthiq.model.Decision;
import com.priorauthiq.retriever.PolicyRetriever;
import com.priorauthiq.store.DeterminationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates prior-authorization triage: resolve the governing policy via
 * the configured {@link PolicyRetriever}, run the configured {@link
 * PolicyMatcher}, persist and return the determination.
 *
 * <p>Both collaborators are injected by interface and selected by
 * configuration — direct lookup or vector retrieval (Slice 3a), deterministic
 * or Spring AI matching (Slice 2) — and this service does not change.
 */
@Service
public class PriorAuthService {

    private static final Logger log = LoggerFactory.getLogger(PriorAuthService.class);

    private final PolicyRetriever policyRetriever;
    private final DeterminationStore determinationStore;
    private final PolicyMatcher policyMatcher;

    public PriorAuthService(PolicyRetriever policyRetriever,
                            DeterminationStore determinationStore,
                            PolicyMatcher policyMatcher) {
        this.policyRetriever = policyRetriever;
        this.determinationStore = determinationStore;
        this.policyMatcher = policyMatcher;
    }

    /**
     * Triage a submitted request.
     *
     * @throws PolicyNotFoundException if no policy governs the requested service
     */
    public Determination triage(PriorAuthRequest submitted) {
        CoveragePolicy policy = policyRetriever.retrieve(submitted)
                .orElseThrow(() -> new PolicyNotFoundException(submitted.serviceCode()));

        PriorAuthRequest request = withServerFields(submitted);
        Determination determination = policyMatcher.evaluate(request, policy);

        log.info("Triaged request {} for {} -> {} [retriever={}, matcher={}]",
                request.requestId(), request.serviceCode(),
                determination.decision(), policyRetriever.strategy(), policyMatcher.strategy());

        return determinationStore.save(determination);
    }

    /** List determinations, newest first, optionally filtered by decision. */
    public List<Determination> list(Decision decision) {
        return determinationStore.findAll(decision);
    }

    /** Fetch a determination by id. */
    public Optional<Determination> findById(String id) {
        return determinationStore.findById(id);
    }

    private PriorAuthRequest withServerFields(PriorAuthRequest submitted) {
        return new PriorAuthRequest(
                UUID.randomUUID().toString(),
                submitted.patientId(),
                submitted.serviceCode(),
                submitted.diagnosisCode(),
                submitted.clinicalNotes(),
                submitted.documentedHistory(),
                Instant.now()
        );
    }
}
