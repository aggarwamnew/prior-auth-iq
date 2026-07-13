package com.priorauthiq.model;

/**
 * The evaluation of one {@link PolicyCriterion} against a request: the outcome
 * plus the evidence the matcher relied on. This is the audit trail — every
 * determination is explainable criterion by criterion.
 *
 * @param criterionId  the criterion evaluated
 * @param description  its human-readable requirement (copied for a self-contained record)
 * @param status       MET / NOT_MET / UNCLEAR
 * @param evidence     the text or reasoning that justified the status
 */
public record CriterionResult(
        String criterionId,
        String description,
        CriterionStatus status,
        String evidence
) {
}
