package com.priorauthiq.controller;

import com.priorauthiq.model.PriorAuthRequest;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request body for submitting a prior-authorization request. Server-assigned
 * fields (request id, timestamp) are not accepted from the client.
 */
public record PriorAuthSubmission(

        @NotBlank(message = "patientId is required")
        String patientId,

        @NotBlank(message = "serviceCode is required")
        String serviceCode,

        @NotBlank(message = "diagnosisCode is required")
        String diagnosisCode,

        @NotBlank(message = "clinicalNotes is required")
        String clinicalNotes,

        List<String> documentedHistory
) {
    /** Convert to a domain request; server fields are populated in the service. */
    public PriorAuthRequest toRequest() {
        return new PriorAuthRequest(
                null, patientId, serviceCode, diagnosisCode,
                clinicalNotes, documentedHistory, null);
    }
}
