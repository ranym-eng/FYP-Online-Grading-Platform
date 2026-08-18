package fyp_grading_platform.grading;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GradingService {
    private final GradeRepository grades;
    private final ConsolidationService consolidation;

    public GradingService(GradeRepository grades, ConsolidationService consolidation) {
        this.grades = grades;
        this.consolidation = consolidation;
    }

    public Grade calculate(UUID projectId, UUID phaseId) {
        consolidation.calculate(projectId, phaseId);
        return grades.findByProjectIdAndPhaseId(projectId, phaseId).orElseThrow();
    }
}