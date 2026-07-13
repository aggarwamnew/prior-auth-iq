package com.priorauthiq.matcher;

import com.priorauthiq.model.CoveragePolicy;
import com.priorauthiq.model.CriterionResult;
import com.priorauthiq.model.CriterionStatus;
import com.priorauthiq.model.Decision;
import com.priorauthiq.model.Determination;
import com.priorauthiq.model.PolicyCriterion;
import com.priorauthiq.model.PriorAuthRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicPolicyMatcherTest {

    private final DeterministicPolicyMatcher matcher = new DeterministicPolicyMatcher();

    private static final CoveragePolicy ADALIMUMAB = new CoveragePolicy(
            "J0135", "Adalimumab (Humira)", "Moderate-to-severe rheumatoid arthritis",
            List.of(
                    new PolicyCriterion("RA-DIAGNOSIS",
                            "Documented moderate-to-severe rheumatoid arthritis",
                            List.of("rheumatoid arthritis")),
                    new PolicyCriterion("RA-STEP-THERAPY",
                            "Trial and failure of methotrexate for at least 3 months",
                            List.of("methotrexate")),
                    new PolicyCriterion("RA-TB-SCREEN",
                            "Tuberculosis screening on file",
                            List.of("tb screening"))
            ));

    private PriorAuthRequest request(List<String> history) {
        return new PriorAuthRequest("req-1", "patient-1", "J0135", "M06.9",
                "Active disease.", history, null);
    }

    @Test
    void approvesWhenAllCriteriaMet() {
        Determination d = matcher.evaluate(request(List.of(
                "Diagnosis: moderate-to-severe rheumatoid arthritis",
                "Tried and failed methotrexate for 4 months",
                "TB screening completed prior to therapy"
        )), ADALIMUMAB);

        assertThat(d.decision()).isEqualTo(Decision.APPROVE);
        assertThat(d.criteriaResults())
                .extracting(CriterionResult::status)
                .containsOnly(CriterionStatus.MET);
        assertThat(d.missingInfo()).isEmpty();
        assertThat(d.reviewed()).isFalse();
    }

    @Test
    void deniesWhenARequiredCriterionIsNegated() {
        Determination d = matcher.evaluate(request(List.of(
                "Diagnosis: moderate-to-severe rheumatoid arthritis",
                "Patient has not tried methotrexate",
                "TB screening completed"
        )), ADALIMUMAB);

        assertThat(d.decision()).isEqualTo(Decision.DENY);
        assertThat(d.criteriaResults())
                .filteredOn(r -> r.criterionId().equals("RA-STEP-THERAPY"))
                .extracting(CriterionResult::status)
                .containsExactly(CriterionStatus.NOT_MET);
    }

    @Test
    void pendsWhenInformationIsMissing() {
        Determination d = matcher.evaluate(request(List.of(
                "Diagnosis: moderate-to-severe rheumatoid arthritis",
                "TB screening completed"
                // methotrexate step therapy not documented
        )), ADALIMUMAB);

        assertThat(d.decision()).isEqualTo(Decision.PEND);
        assertThat(d.missingInfo())
                .containsExactly("Trial and failure of methotrexate for at least 3 months");
    }

    @Test
    void reportsItsStrategy() {
        assertThat(matcher.strategy()).isEqualTo("deterministic-keyword-v1");
    }
}
