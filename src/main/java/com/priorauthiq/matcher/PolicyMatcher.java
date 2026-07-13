package com.priorauthiq.matcher;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PriorAuthRequest;

/**
 * Evaluates a prior-authorization request against a coverage policy and
 * produces a {@link Determination}.
 *
 * <p>This is the seam the whole project is built around. Slice 1 ships a
 * {@link DeterministicPolicyMatcher} (transparent, keyword-based, no model) so
 * the service is provably correct end-to-end. Slice 2 adds a Spring AI
 * {@code ChatClient}-backed implementation that reasons over the criteria and
 * returns the same {@code Determination} via structured output — selected by
 * configuration, with the rest of the system unchanged.
 */
public interface PolicyMatcher {

    /**
     * @param request the submitted prior-auth request
     * @param policy  the coverage policy governing the requested service
     * @return a recommended determination with a per-criterion audit trail
     */
    Determination evaluate(PriorAuthRequest request, CoveragePolicy policy);

    /** Identifies the strategy in logs, metrics, and the API response. */
    String strategy();
}
