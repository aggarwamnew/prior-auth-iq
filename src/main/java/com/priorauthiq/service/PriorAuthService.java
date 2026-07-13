package com.priorauthiq.service;

import com.priorauthiq.matcher.PolicyMatcher;
import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PriorAuthRequest;
import com.priorauthiq.model.Decision;
import com.priorauthiq.store.DeterminationStore;
import com.priorauthiq.store.PolicyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates prior-authorization triage: resolve the governing policy, run
 * the configured {@link PolicyMatcher}, persist and return the determination.
 *
 * <p>The matcher is injected by interface — the deterministic (Slice 1) or the
 * Spring AI (Slice 2) implementation is selected by configuration, and this
 * service does not change.
 */
@Service
public class PriorAuthService {

    private static final Logger log = LoggerFactory.getLogger(PriorAuthService.class);

    private final PolicyStore policyStore;
    private final DeterminationStore determinationStore;
    private final PolicyMatcher policyMatcher;

    public PriorAuthService(PolicyStore policyStore,
                            DeterminationStore determinationStore,
                            PolicyMatcher policyMatcher) {
        this.policyStore = policyStore;
        this.determinationStore = determinationStore;
        this.policyMatcher = policyMatcher;
    }

    /**
     * Triage a submitted request.
     *
     * @throws PolicyNotFoundException if no policy governs the requested service
     */
    public Determination triage(PriorAuthRequest submitted) {
        CoveragePolicy policy = policyStore.findByServiceCode(submitted.serviceCode())
                .orElseThrow(() -> new PolicyNotFoundException(submitted.serviceCode()));

        PriorAuthRequest request = withServerFields(submitted);
        Determination determination = policyMatcher.evaluate(request, policy);

        log.info("Triaged request {} for {} -> {} [{}]",
                request.requestId(), request.serviceCode(),
                determination.decision(), policyMatcher.strategy());

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
