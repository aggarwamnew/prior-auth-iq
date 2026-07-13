package com.priorauthiq.matcher;

import com.priorauthiq.model.CriterionStatus;
import com.priorauthiq.model.Decision;

import java.util.List;

/**
 * The structured shape the LLM is asked to return — the model's <em>judgment</em>
 * only. Server-assigned metadata (id, timestamps, reviewed flag) is added by
 * {@link SpringAiPolicyMatcher}, never by the model.
 *
 * <p>Spring AI's structured-output converter generates a JSON schema from this
 * record, instructs the model to produce matching JSON, and deserializes the
 * response — so the LLM path returns a typed, validated object, never free text.
 *
 * @param decision    overall recommendation
 * @param criteria    per-criterion assessment
 * @param missingInfo for PEND: what documentation is needed
 * @param rationale   one-paragraph human-readable summary
 */
public record LlmAssessment(
        Decision decision,
        List<Assessed> criteria,
        List<String> missingInfo,
        String rationale
) {
    /**
     * @param criterionId the policy criterion id being assessed
     * @param status      MET / NOT_MET / UNCLEAR
     * @param evidence    the text from the request that justifies the status
     */
    public record Assessed(String criterionId, CriterionStatus status, String evidence) {
    }
}
