package com.priorauthiq.retriever;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.PriorAuthRequest;

import java.util.Optional;

/**
 * Resolves the coverage policy that governs a prior-auth request.
 *
 * <p>The second seam of the architecture, alongside {@link
 * com.priorauthiq.matcher.PolicyMatcher}. Slice 1 resolved policies by exact
 * service-code lookup; Slice 3a adds a retrieval-augmented implementation that
 * searches an embedded policy corpus. The service layer depends only on this
 * interface, so the resolution strategy is a configuration choice.
 */
public interface PolicyRetriever {

    /**
     * @param request the submitted prior-auth request
     * @return the governing policy, or empty if none is found (which the
     *         service surfaces as a policy-not-found error)
     */
    Optional<CoveragePolicy> retrieve(PriorAuthRequest request);

    /** Identifies the strategy in logs and diagnostics. */
    String strategy();
}
