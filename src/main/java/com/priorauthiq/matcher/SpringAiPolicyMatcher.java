package com.priorauthiq.matcher;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.CriterionResult;
import com.priorauthiq.model.CriterionStatus;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PolicyCriterion;
import com.priorauthiq.model.PriorAuthRequest;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Slice-2 matcher: a Spring AI {@link ChatClient} reasons over the coverage
 * criteria and returns a structured {@link LlmAssessment}, which is wrapped into
 * the same {@link Determination} the deterministic matcher produces.
 *
 * <p>Provider-portable: the injected {@code ChatClient} is backed by whatever
 * model provider is configured (OpenAI, Anthropic, Ollama, Azure...) — this
 * class does not know or care which. Selected via {@code priorauthiq.matcher=llm}.
 *
 * <p>The model supplies only the judgment (decision + per-criterion assessment
 * + rationale); server metadata (id, timestamps, {@code reviewed=false}) is
 * added here. The human-in-the-loop guarantee is structural: the assistant
 * recommends, a reviewer signs.
 */
public class SpringAiPolicyMatcher implements PolicyMatcher {

    private static final String SYSTEM = """
            You are a prior-authorization triage assistant for a health insurer.
            You do NOT make medical decisions. You check whether a request's
            documentation satisfies each written coverage criterion, exactly as a
            human reviewer would, and recommend APPROVE, DENY, or PEND:
              - APPROVE: every criterion is MET.
              - DENY: a required criterion is definitively NOT_MET.
              - PEND: one or more criteria are UNCLEAR (info missing); list what
                documentation is needed in missingInfo.
            Assess each criterion as MET, NOT_MET, or UNCLEAR, citing the exact
            evidence from the request. Do not infer facts that are not documented.
            """;

    private final ChatClient chatClient;

    public SpringAiPolicyMatcher(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Determination evaluate(PriorAuthRequest request, CoveragePolicy policy) {
        LlmAssessment assessment = chatClient.prompt()
                .system(SYSTEM)
                .user(userPrompt(request, policy))
                .call()
                .entity(LlmAssessment.class);
        return toDetermination(assessment, request, policy);
    }

    @Override
    public String strategy() {
        return "spring-ai-llm-v1";
    }

    /** The per-request prompt: the coverage criteria and the submitted evidence. */
    String userPrompt(PriorAuthRequest request, CoveragePolicy policy) {
        StringBuilder sb = new StringBuilder();
        sb.append("Coverage policy: ").append(policy.serviceName())
                .append(" (").append(policy.indication()).append(")\n");
        sb.append("Criteria (all must be MET to approve):\n");
        for (PolicyCriterion c : policy.criteria()) {
            sb.append("  - [").append(c.id()).append("] ").append(c.description()).append('\n');
        }
        sb.append("\nRequest:\n");
        sb.append("  Diagnosis code: ").append(request.diagnosisCode()).append('\n');
        sb.append("  Clinical notes: ").append(request.clinicalNotes()).append('\n');
        if (!request.documentedHistory().isEmpty()) {
            sb.append("  Documented history:\n");
            request.documentedHistory().forEach(h -> sb.append("    - ").append(h).append('\n'));
        }
        sb.append("\nAssess each criterion by its id and recommend a decision.");
        return sb.toString();
    }

    /** Wrap the model's judgment into a full, auditable Determination. */
    Determination toDetermination(LlmAssessment assessment, PriorAuthRequest request, CoveragePolicy policy) {
        Map<String, String> descById = policy.criteria().stream()
                .collect(Collectors.toMap(PolicyCriterion::id, PolicyCriterion::description, (a, b) -> a));

        List<CriterionResult> results = new ArrayList<>();
        List<LlmAssessment.Assessed> assessed = assessment.criteria() == null ? List.of() : assessment.criteria();
        for (LlmAssessment.Assessed a : assessed) {
            String desc = descById.getOrDefault(a.criterionId(), a.criterionId());
            CriterionStatus status = a.status() == null ? CriterionStatus.UNCLEAR : a.status();
            results.add(new CriterionResult(a.criterionId(), desc, status, a.evidence()));
        }

        return new Determination(
                UUID.randomUUID().toString(),
                request.requestId(),
                request.serviceCode(),
                assessment.decision(),
                results,
                assessment.missingInfo() == null ? List.of() : assessment.missingInfo(),
                assessment.rationale(),
                false,
                Instant.now()
        );
    }
}
