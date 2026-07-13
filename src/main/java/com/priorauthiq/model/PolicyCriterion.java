package com.priorauthiq.model;

import java.util.List;

/**
 * A single, checkable requirement within a coverage policy.
 *
 * @param id          stable identifier, e.g. {@code "RA-STEP-THERAPY"}
 * @param description human-readable requirement the reviewer checks against
 * @param keywords    slice-1 hints for the deterministic matcher; from Slice 2
 *                    the LLM matcher reasons over {@code description} directly
 *                    and these become optional
 */
public record PolicyCriterion(
        String id,
        String description,
        List<String> keywords
) {
    public PolicyCriterion {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
