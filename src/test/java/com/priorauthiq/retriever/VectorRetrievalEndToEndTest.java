package com.priorauthiq.retriever;

import com.priorauthiq.rag.FakeEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test of the RAG path (Slice 3a): {@code priorauthiq.retriever=
 * vector} with the deterministic {@link FakeEmbeddingModel} standing in for a
 * provider's embedding model. The seeded corpus is ingested at startup, the
 * request is resolved by clinical similarity — not by key — and the rest of
 * the flow (deterministic matcher, determination, store) is unchanged.
 */
@SpringBootTest(properties = "priorauthiq.retriever=vector")
@AutoConfigureMockMvc
@Import(VectorRetrievalEndToEndTest.FakeEmbeddingConfig.class)
class VectorRetrievalEndToEndTest {

    @TestConfiguration
    static class FakeEmbeddingConfig {
        @Bean
        EmbeddingModel embeddingModel() {
            return new FakeEmbeddingModel();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private static final String APPROVABLE = """
            {
              "patientId": "patient-1",
              "serviceCode": "J0135",
              "diagnosisCode": "M06.9",
              "clinicalNotes": "Moderate-to-severe rheumatoid arthritis, biologic-naive.",
              "documentedHistory": [
                "Diagnosis: moderate-to-severe rheumatoid arthritis",
                "Tried and failed methotrexate for 4 months",
                "TB screening completed prior to therapy"
              ]
            }
            """;

    @Test
    void triagesViaVectorRetrievalEndToEnd() throws Exception {
        mockMvc.perform(post("/api/prior-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(APPROVABLE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceCode").value("J0135"))
                .andExpect(jsonPath("$.decision").value("APPROVE"));
    }
}
