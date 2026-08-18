package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
public class EvaluationSheetCalculator {
    private static final Map<String, Double> PRESENTATION_INDIVIDUAL = Map.of(
            "present-information", 1.0,
            "answer-questions", 4.0
    );
    private static final Map<String, Double> PRESENTATION_GROUP = Map.of(
            "technical-presentation", 1.0,
            "identify-problem", 1.0,
            "formulate-problem", 1.0,
            "design-requirements", 1.0,
            "analyze-solutions", 2.0,
            "evaluate-ethics-impact", 1.5,
            "complete-work", 2.0
    );
    private static final Map<String, Double> REPORT = Map.ofEntries(
            Map.entry("identify-problem", 1.0),
            Map.entry("formulate-problem", 1.0),
            Map.entry("design-requirements", 1.0),
            Map.entry("analyze-solutions", 2.0),
            Map.entry("develop-solutions", 1.0),
            Map.entry("build-test", 1.0),
            Map.entry("technical-report", 1.0),
            Map.entry("professional-ethics", 1.0),
            Map.entry("evaluate-impact", 1.0),
            Map.entry("complete-work", 1.0)
    );
    private static final Map<String, Double> SUPERVISOR = Map.ofEntries(
            Map.entry("analyze-solutions", 1.0),
            Map.entry("build-test", 1.0),
            Map.entry("professional-responsibility", 1.0),
            Map.entry("plan-objectives", 1.0),
            Map.entry("assigned-tasks", 1.0),
            Map.entry("team-leadership", 1.0),
            Map.entry("acquire-information", 1.0),
            Map.entry("learning-strategies", 1.0),
            Map.entry("apply-knowledge", 1.0),
            Map.entry("technical-questions", 1.0),
            Map.entry("proposal-deadline", 1.0)
    );
    private static final Map<String, Double> DEMO = Map.of(
            "prototype", 2.0,
            "present-prototype", 1.0,
            "answer-questions", 4.0,
            "complete-work", 2.0,
            "poster", 1.0
    );

    public double calculate(EvaluationType type, Map<String, Double> scores) {
        validateValues(scores);
        return switch (type) {
            case ORAL_PHASE_I, ORAL_PHASE_II -> presentation(scores);
            case REPORT_PHASE_I, REPORT_PHASE_II -> report(scores);
            case SUPERVISOR_PHASE_I, SUPERVISOR_PHASE_II -> individualAverage(scores, SUPERVISOR);
            case DEMO_DAY_INDUSTRY -> weightedAverage(scores, "group", "group", DEMO);
        };
    }

    public double calculateForTarget(EvaluationType type, Map<String, Double> scores, String studentId) {
        validateValues(scores);
        return switch (type) {
            case ORAL_PHASE_I, ORAL_PHASE_II -> presentationForTarget(scores, studentId);
            case REPORT_PHASE_I, REPORT_PHASE_II -> scores.keySet().stream().anyMatch(key -> key.startsWith("group:"))
                    ? weightedAverage(scores, "group", "group", REPORT)
                    : weightedAverage(scores, "individual", studentId, REPORT);
            case SUPERVISOR_PHASE_I, SUPERVISOR_PHASE_II ->
                    weightedAverage(scores, "individual", studentId, SUPERVISOR);
            case DEMO_DAY_INDUSTRY -> weightedAverage(scores, "group", "group", DEMO);
        };
    }

    public Set<String> expectedScoreKeys(EvaluationType type, Collection<String> studentIds) {
        Set<String> expected = new LinkedHashSet<>();
        switch (type) {
            case ORAL_PHASE_I, ORAL_PHASE_II -> {
                requireStudents(studentIds);
                addIndividualKeys(expected, studentIds, PRESENTATION_INDIVIDUAL);
                addGroupKeys(expected, PRESENTATION_GROUP);
            }
            case REPORT_PHASE_I, REPORT_PHASE_II -> addGroupKeys(expected, REPORT);
            case SUPERVISOR_PHASE_I, SUPERVISOR_PHASE_II -> {
                requireStudents(studentIds);
                addIndividualKeys(expected, studentIds, SUPERVISOR);
            }
            case DEMO_DAY_INDUSTRY -> addGroupKeys(expected, DEMO);
        }
        return expected;
    }

    public void validateScoreKeys(EvaluationType type, Map<String, Double> scores, Collection<String> studentIds) {
        validateValues(scores);
        Set<String> expected = expectedScoreKeys(type, studentIds);
        scores.keySet().stream()
                .filter(key -> !expected.contains(key))
                .findFirst()
                .ifPresent(key -> {
                    throw new BusinessException("UNEXPECTED_SCORE_KEY", "Unexpected score criterion: " + key);
                });
    }

    private double presentation(Map<String, Double> scores) {
        Set<String> targets = targets(scores, "individual");
        double group = weightedAverage(scores, "group", "group", PRESENTATION_GROUP);
        if (targets.isEmpty()) return group * (25.0 / 40.0);
        return targets.stream()
                .mapToDouble(target -> weightedAverage(scores, "individual", target, PRESENTATION_INDIVIDUAL)
                        * (15.0 / 40.0) + group * (25.0 / 40.0))
                .average()
                .orElse(0);
    }

    private double presentationForTarget(Map<String, Double> scores, String studentId) {
        double individual = weightedAverage(scores, "individual", studentId, PRESENTATION_INDIVIDUAL);
        double group = weightedAverage(scores, "group", "group", PRESENTATION_GROUP);
        return Math.round((individual * (15.0 / 40.0) + group * (25.0 / 40.0)) * 100.0) / 100.0;
    }

    private double report(Map<String, Double> scores) {
        if (scores.keySet().stream().anyMatch(key -> key.startsWith("group:"))) {
            return weightedAverage(scores, "group", "group", REPORT);
        }
        // Backward compatibility for drafts created before reports became project-level forms.
        return individualAverage(scores, REPORT);
    }

    private double individualAverage(Map<String, Double> scores, Map<String, Double> weights) {
        Set<String> targets = targets(scores, "individual");
        return targets.stream()
                .mapToDouble(target -> weightedAverage(scores, "individual", target, weights))
                .average()
                .orElse(0);
    }

    private double weightedAverage(
            Map<String, Double> scores,
            String section,
            String target,
            Map<String, Double> weights
    ) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) return 0;
        double weighted = weights.entrySet().stream()
                .mapToDouble(entry -> scores.getOrDefault(
                        section + ":" + entry.getKey() + ":" + target,
                        0.0
                ) * entry.getValue())
                .sum();
        return Math.round((weighted / totalWeight) * 100.0) / 100.0;
    }

    private Set<String> targets(Map<String, Double> scores, String section) {
        Set<String> targets = new LinkedHashSet<>();
        scores.keySet().forEach(key -> {
            String[] parts = key.split(":", 3);
            if (parts.length == 3 && parts[0].equals(section)) targets.add(parts[2]);
        });
        return targets;
    }

    private void validateValues(Map<String, Double> scores) {
        scores.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || !Double.isFinite(value)
                    || value < 0 || value > 10) {
                throw new BusinessException("INVALID_SCORE", "Each score must be between 0 and 10");
            }
        });
    }

    private void requireStudents(Collection<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BusinessException("TEAM_HAS_NO_STUDENTS", "The project team must contain students before evaluation");
        }
    }

    private void addIndividualKeys(Set<String> target, Collection<String> studentIds, Map<String, Double> criteria) {
        for (String studentId : studentIds) {
            criteria.keySet().forEach(criterion -> target.add("individual:" + criterion + ":" + studentId));
        }
    }

    private void addGroupKeys(Set<String> target, Map<String, Double> criteria) {
        criteria.keySet().forEach(criterion -> target.add("group:" + criterion + ":group"));
    }
}
