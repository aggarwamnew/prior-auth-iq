package com.priorauthiq.store;

import com.priorauthiq.model.Decision;
import com.priorauthiq.model.Determination;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory store of determinations produced by the assistant. Every triage
 * decision is retained for audit and reviewer sign-off.
 */
@Repository
public class DeterminationStore {

    private final ConcurrentMap<String, Determination> byId = new ConcurrentHashMap<>();

    public Determination save(Determination determination) {
        byId.put(determination.id(), determination);
        return determination;
    }

    public Optional<Determination> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** All determinations, newest first, optionally filtered by decision. */
    public List<Determination> findAll(Decision decisionFilter) {
        return byId.values().stream()
                .filter(d -> decisionFilter == null || d.decision() == decisionFilter)
                .sorted(Comparator.comparing(Determination::determinedAt).reversed())
                .toList();
    }
}
