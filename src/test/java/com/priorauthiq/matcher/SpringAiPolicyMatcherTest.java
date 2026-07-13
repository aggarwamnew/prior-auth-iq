package com.priorauthiq.matcher;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.CriterionResult;
import com.priorauthiq.model.CriterionStatus;
import com.priorauthiq.model.Decision;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PolicyCriterion;
import com.priorauthiq.model.PriorAuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiPolicyMatcherTest {

    private static final CoveragePolicy POLICY = new CoveragePolicy(
            "J0135", "Adalimumab (Humira)", "Moderate-to-severe rheumatoid arthritis",
            List.of(
                    new PolicyCriterion("RA-DIAGNOSIS", "Documented moderate-to-severe rheumatoid arthritis", List.of()),
                    new PolicyCriterion("RA-STEP-THERAPY", "Trial and failure of methotrexate for at least 3 months", List.of()),
                    new PolicyCriterion("RA-TB-SCREEN", "Tuberculosis screening on file", List.of())
            ));

    private PriorAuthRequest request() {
        return new PriorAuthRequest("req-1", "patient-1", "J0135", "M06.9",
                "Active disease.", List.of("methotrexate failed", "TB screening done"), null);
    }

    // ── Pure mapping: LlmAssessment -> Determination (no ChatClient) ──────────

    @Test
    void wrapsModelJudgmentIntoAuditableDetermination() {
        LlmAssessment assessment = new LlmAssessment(
                Decision.APPROVE,
                List.of(
                        new LlmAssessment.Assessed("RA-DIAGNOSIS", CriterionStatus.MET, "moderate-to-severe RA documented"),
                        new LlmAssessment.Assessed("RA-STEP-THERAPY", CriterionStatus.MET, "methotrexate tried and failed"),
                        new LlmAssessment.Assessed("RA-TB-SCREEN", CriterionStatus.MET, "TB screening on file")
                ),
                List.of(),
                "All criteria met.");

        SpringAiPolicyMatcher matcher = new SpringAiPolicyMatcher(null);
        Determination d = matcher.toDetermination(assessment, request(), POLICY);

        assertThat(d.decision()).isEqualTo(Decision.APPROVE);
        assertThat(d.requestId()).isEqualTo("req-1");
        assertThat(d.serviceCode()).isEqualTo("J0135");
        assertThat(d.reviewed()).isFalse();               // human-in-the-loop preserved
        assertThat(d.id()).isNotBlank();                  // server-assigned, not from the model
        assertThat(d.criteriaResults())
                .extracting(CriterionResult::status)
                .containsOnly(CriterionStatus.MET);
        // description is pulled from the policy, not trusted from the model
        assertThat(d.criteriaResults())
                .filteredOn(r -> r.criterionId().equals("RA-STEP-THERAPY"))
                .extracting(CriterionResult::description)
                .containsExactly("Trial and failure of methotrexate for at least 3 months");
    }

    @Test
    void userPromptCarriesCriteriaAndEvidence() {
        SpringAiPolicyMatcher matcher = new SpringAiPolicyMatcher(null);
        String prompt = matcher.userPrompt(request(), POLICY);

        assertThat(prompt)
                .contains("RA-STEP-THERAPY")
                .contains("Trial and failure of methotrexate")
                .contains("Active disease.")
                .contains("methotrexate failed");
    }

    // ── End-to-end through a real ChatClient over a mocked ChatModel ──────────

    @Test
    void evaluatesViaChatClientStructuredOutput() {
        String modelJson = """
                {
                  "decision": "PEND",
                  "criteria": [
                    {"criterionId": "RA-DIAGNOSIS", "status": "MET", "evidence": "RA documented"},
                    {"criterionId": "RA-STEP-THERAPY", "status": "UNCLEAR", "evidence": "not documented"}
                  ],
                  "missingInfo": ["methotrexate trial documentation"],
                  "rationale": "Step therapy not documented."
                }
                """;

        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(modelJson)))));

        SpringAiPolicyMatcher matcher = new SpringAiPolicyMatcher(ChatClient.create(chatModel));
        Determination d = matcher.evaluate(request(), POLICY);

        assertThat(d.decision()).isEqualTo(Decision.PEND);
        assertThat(d.missingInfo()).containsExactly("methotrexate trial documentation");
        assertThat(d.reviewed()).isFalse();
        assertThat(matcher.strategy()).isEqualTo("spring-ai-llm-v1");
    }
}
