package com.priorauthiq.model;

import java.time.Instant;
import java.util.List;

/**
 * A submitted prior-authorization request: what the provider is asking the
 * payer to pre-approve, plus the clinical justification.
 *
 * @param requestId        server-assigned identifier
 * @param patientId        opaque patient reference (no PHI in this demo — all data is synthetic)
 * @param serviceCode      the requested service/drug code; keys into a {@link CoveragePolicy}
 * @param diagnosisCode    ICD-10 diagnosis code
 * @param clinicalNotes    free-text justification from the provider
 * @param documentedHistory discrete documented facts (e.g. prior therapies tried)
 * @param submittedAt      submission timestamp
 */
public record PriorAuthRequest(
        String requestId,
        String patientId,
        String serviceCode,
        String diagnosisCode,
        String clinicalNotes,
        List<String> documentedHistory,
        Instant submittedAt
) {
    public PriorAuthRequest {
        documentedHistory = documentedHistory == null ? List.of() : List.copyOf(documentedHistory);
    }

    /**
     * All provider-supplied evidence as one lowercased blob — the surface the
     * slice-1 deterministic matcher scans. The LLM matcher (Slice 2) consumes
     * the structured fields directly instead.
     */
    public String evidenceText() {
        StringBuilder sb = new StringBuilder();
        if (clinicalNotes != null) {
            sb.append(clinicalNotes).append('\n');
        }
        documentedHistory.forEach(h -> sb.append(h).append('\n'));
        return sb.toString().toLowerCase();
    }
}
