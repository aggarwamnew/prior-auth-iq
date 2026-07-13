package com.priorauthiq.model;

import java.time.Instant;
import java.util.List;

/**
 * The assistant's recommended triage outcome for a request, with a full,
 * auditable evidence trail. Awaits human sign-off (see {@link #reviewed}).
 *
 * <p>This is exactly the shape the Slice-2 LLM matcher returns via Spring AI
 * structured output — the deterministic matcher produces the same record so
 * the swap is transparent to the rest of the system.
 *
 * @param id             determination identifier
 * @param requestId      the request this evaluates
 * @param serviceCode    the requested item
 * @param decision       APPROVE / DENY / PEND
 * @param criteriaResults per-criterion evaluation (the audit trail)
 * @param missingInfo    for PEND: exactly what documentation is needed
 * @param rationale      one-paragraph human-readable summary
 * @param reviewed       false until a human reviewer signs off
 * @param determinedAt   when the assistant produced this
 */
public record Determination(
        String id,
        String requestId,
        String serviceCode,
        Decision decision,
        List<CriterionResult> criteriaResults,
        List<String> missingInfo,
        String rationale,
        boolean reviewed,
        Instant determinedAt
) {
    public Determination {
        criteriaResults = criteriaResults == null ? List.of() : List.copyOf(criteriaResults);
        missingInfo = missingInfo == null ? List.of() : List.copyOf(missingInfo);
    }
}
