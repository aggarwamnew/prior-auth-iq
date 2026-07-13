package com.priorauthiq.model;

/**
 * Whether a single coverage-policy criterion is satisfied by a request.
 */
public enum CriterionStatus {

    /** The request documents that this criterion is satisfied. */
    MET,

    /** The request documents that this criterion is definitively not satisfied. */
    NOT_MET,

    /** The request does not contain enough information to decide. */
    UNCLEAR
}
