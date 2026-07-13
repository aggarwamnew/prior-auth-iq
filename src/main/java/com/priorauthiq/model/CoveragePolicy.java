package com.priorauthiq.model;

import java.util.List;

/**
 * A payer's coverage policy for one service or drug: the set of criteria a
 * prior-authorization request must satisfy to be approved.
 *
 * <p>In Slice 3 this corpus moves into a vector store and is retrieved via RAG;
 * for now policies are held in-memory (see {@code PolicyStore}).
 *
 * @param serviceCode canonical code for the requested item (e.g. HCPCS
 *                    {@code "J0135"} for adalimumab)
 * @param serviceName human-readable name
 * @param indication  the approved clinical indication this policy governs
 * @param criteria    ordered list of requirements; all must be MET to approve
 */
public record CoveragePolicy(
        String serviceCode,
        String serviceName,
        String indication,
        List<PolicyCriterion> criteria
) {
    public CoveragePolicy {
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
    }
}
