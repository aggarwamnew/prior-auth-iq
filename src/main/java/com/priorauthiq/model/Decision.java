package com.priorauthiq.model;

/**
 * The triage outcome of a prior-authorization request.
 *
 * <p>PriorAuthIQ is a decision-<em>support</em> system: it produces a
 * recommended {@code Decision} with a full evidence trail, but a human
 * reviewer always signs the final determination. The assistant never makes a
 * medical judgment — it checks a request against written coverage criteria.
 */
public enum Decision {

    /** Every required criterion is met — recommend approval. */
    APPROVE,

    /** A required criterion is definitively not met — recommend denial. */
    DENY,

    /** Information is missing or unclear — pend for more documentation. */
    PEND
}
