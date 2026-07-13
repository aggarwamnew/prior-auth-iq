package com.priorauthiq.matcher;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.CriterionResult;
import com.priorauthiq.model.CriterionStatus;
import com.priorauthiq.model.Decision;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PolicyCriterion;
import com.priorauthiq.model.PriorAuthRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Slice-1 baseline matcher: transparent keyword-and-negation scanning, no LLM.
 *
 * <p>It exists so the service is provably correct end-to-end before any model
 * is wired in, and so Slice 2 has a deterministic control to evaluate the LLM
 * matcher against. It is deliberately naive: it reasons at the level of keyword
 * presence and simple negation, not clinical meaning. Genuinely ambiguous notes
 * fall to {@code UNCLEAR} → {@code PEND}, which is the safe default — the
 * baseline never fabricates confidence it does not have. Nuanced judgment
 * (partial therapy durations, implicit criteria, contradictory notes) is what
 * the Slice-2 reasoning matcher is for.
 */
public class DeterministicPolicyMatcher implements PolicyMatcher {

    private static final List<String> NEGATIONS =
            List.of("did not", "does not", "not ", "no ", "without", "never", "denies", "unable to");

    @Override
    public Determination evaluate(PriorAuthRequest request, CoveragePolicy policy) {
        List<String> evidenceLines = evidenceLines(request);
        List<CriterionResult> results = new ArrayList<>();

        for (PolicyCriterion criterion : policy.criteria()) {
            results.add(evaluateCriterion(criterion, evidenceLines));
        }

        Decision decision = decide(results);
        List<String> missingInfo = results.stream()
                .filter(r -> r.status() == CriterionStatus.UNCLEAR)
                .map(CriterionResult::description)
                .toList();

        return new Determination(
                UUID.randomUUID().toString(),
                request.requestId(),
                request.serviceCode(),
                decision,
                results,
                missingInfo,
                rationale(decision, policy, results, missingInfo),
                false,
                Instant.now()
        );
    }

    @Override
    public String strategy() {
        return "deterministic-keyword-v1";
    }

    private CriterionResult evaluateCriterion(PolicyCriterion criterion, List<String> evidenceLines) {
        String positiveEvidence = null;
        for (String line : evidenceLines) {
            String lower = line.toLowerCase();
            for (String keyword : criterion.keywords()) {
                if (lower.contains(keyword.toLowerCase())) {
                    if (hasNegation(lower)) {
                        return new CriterionResult(
                                criterion.id(), criterion.description(),
                                CriterionStatus.NOT_MET, line.trim());
                    }
                    positiveEvidence = line.trim();
                }
            }
        }
        if (positiveEvidence != null) {
            return new CriterionResult(
                    criterion.id(), criterion.description(),
                    CriterionStatus.MET, positiveEvidence);
        }
        return new CriterionResult(
                criterion.id(), criterion.description(),
                CriterionStatus.UNCLEAR, "no supporting documentation found");
    }

    private Decision decide(List<CriterionResult> results) {
        boolean anyNotMet = results.stream().anyMatch(r -> r.status() == CriterionStatus.NOT_MET);
        if (anyNotMet) {
            return Decision.DENY;
        }
        boolean allMet = results.stream().allMatch(r -> r.status() == CriterionStatus.MET);
        return allMet ? Decision.APPROVE : Decision.PEND;
    }

    private boolean hasNegation(String lowerLine) {
        return NEGATIONS.stream().anyMatch(lowerLine::contains);
    }

    private List<String> evidenceLines(PriorAuthRequest request) {
        List<String> lines = new ArrayList<>();
        if (request.clinicalNotes() != null) {
            for (String line : request.clinicalNotes().split("\\R")) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        lines.addAll(request.documentedHistory());
        return lines;
    }

    private String rationale(Decision decision, CoveragePolicy policy,
                             List<CriterionResult> results, List<String> missingInfo) {
        long met = results.stream().filter(r -> r.status() == CriterionStatus.MET).count();
        return switch (decision) {
            case APPROVE -> "All %d criteria for %s (%s) are met. Recommend approval, pending reviewer sign-off."
                    .formatted(results.size(), policy.serviceName(), policy.indication());
            case DENY -> "A required criterion for %s is not met based on the submitted documentation. Recommend denial, pending reviewer sign-off."
                    .formatted(policy.serviceName());
            case PEND -> "%d of %d criteria met; %d unclear. Pend for: %s."
                    .formatted(met, results.size(), missingInfo.size(), String.join("; ", missingInfo));
        };
    }
}
