package com.priorauthiq.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test: real controller, service, seeded PolicyStore, and the
 * deterministic matcher — the whole Slice-1 flow through HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PriorAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String APPROVABLE = """
            {
              "patientId": "patient-1",
              "serviceCode": "J0135",
              "diagnosisCode": "M06.9",
              "clinicalNotes": "Active disease, biologic-naive.",
              "documentedHistory": [
                "Diagnosis: moderate-to-severe rheumatoid arthritis",
                "Tried and failed methotrexate for 4 months",
                "TB screening completed prior to therapy"
              ]
            }
            """;

    private static final String UNKNOWN_SERVICE = """
            {
              "patientId": "patient-2",
              "serviceCode": "ZZZ99",
              "diagnosisCode": "M06.9",
              "clinicalNotes": "n/a",
              "documentedHistory": []
            }
            """;

    private static final String MISSING_FIELDS = """
            {
              "patientId": "patient-3",
              "serviceCode": "J0135"
            }
            """;

    @Test
    void submitReturnsCreatedWithDetermination() throws Exception {
        mockMvc.perform(post("/api/prior-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVABLE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.decision").value("APPROVE"))
                .andExpect(jsonPath("$.serviceCode").value("J0135"))
                .andExpect(jsonPath("$.criteriaResults").isArray())
                .andExpect(jsonPath("$.reviewed").value(false));
    }

    @Test
    void unknownServiceReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/prior-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UNKNOWN_SERVICE))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingRequiredFieldsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/prior-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MISSING_FIELDS))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAndFetchDeterminations() throws Exception {
        String location = mockMvc.perform(post("/api/prior-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVABLE))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/determinations").param("decision", "APPROVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // location contains the JSON body; pull the id for the fetch-by-id path
        String id = location.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
        mockMvc.perform(get("/api/determinations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }
}
