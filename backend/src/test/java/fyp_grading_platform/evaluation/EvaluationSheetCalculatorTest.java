package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvaluationSheetCalculatorTest {
    private final EvaluationSheetCalculator calculator = new EvaluationSheetCalculator();

    @Test
    void calculatesDemoDayWeightedScore() {
        double result = calculator.calculate(EvaluationType.DEMO_DAY_INDUSTRY, Map.of(
                "group:prototype:group", 8.0,
                "group:present-prototype:group", 7.0,
                "group:answer-questions:group", 9.0,
                "group:complete-work:group", 6.0,
                "group:poster:group", 10.0
        ));

        assertEquals(8.1, result);
    }

    @Test
    void missingDraftValuesRemainZeroUntilSubmission() {
        double result = calculator.calculate(EvaluationType.REPORT_PHASE_I, Map.of(
                "individual:identify-problem:student-1", 10.0
        ));

        assertEquals(0.91, result);
    }

    @Test
    void rejectsScoreOutsideAllowedRange() {
        assertThrows(BusinessException.class, () -> calculator.calculate(
                EvaluationType.DEMO_DAY_INDUSTRY,
                Map.of("group:prototype:group", 11.0)
        ));
    }
}
