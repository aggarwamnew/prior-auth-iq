package com.priorauthiq.retriever;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.PriorAuthRequest;
import com.priorauthiq.store.PolicyStore;

import java.util.Optional;

/**
 * Exact service-code lookup against the in-memory policy store — the Slice-1
 * behaviour, kept permanently as the deterministic control the vector
 * retriever is measured against (same role the deterministic matcher plays
 * for the LLM matcher).
 */
public class DirectPolicyRetriever implements PolicyRetriever {

    private final PolicyStore policyStore;

    public DirectPolicyRetriever(PolicyStore policyStore) {
        this.policyStore = policyStore;
    }

    @Override
    public Optional<CoveragePolicy> retrieve(PriorAuthRequest request) {
        return policyStore.findByServiceCode(request.serviceCode());
    }

    @Override
    public String strategy() {
        return "direct";
    }
}
