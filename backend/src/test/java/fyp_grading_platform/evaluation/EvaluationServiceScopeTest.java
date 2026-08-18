package fyp_grading_platform.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.PhaseWindowService;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EvaluationServiceScopeTest {
    private final EvaluationService service = new EvaluationService(
            mock(EvaluationSubmissionRepository.class),
            mock(CriterionScoreRepository.class),
            mock(RubricCriterionRepository.class),
            mock(ProjectRepository.class),
            mock(TeamRepository.class),
            mock(PhaseRepository.class),
            mock(EvaluationFormTemplateRepository.class),
            mock(EvaluatorProfileRepository.class),
            mock(ProjectEvaluatorAssignmentRepository.class),
            mock(PhaseWindowService.class),
            mock(EvaluationSheetCalculator.class),
            new ObjectMapper()
    );

    @Test
    void industryRepresentativeCanEvaluateDemoDayInPhaseTwo() {
        assertDoesNotThrow(() -> service.assertEvaluationScope(
                actor(UserRole.INDUSTRY_REPRESENTATIVE),
                EvaluationType.DEMO_DAY_INDUSTRY,
                phase(PhaseType.PHASE_II)
        ));
    }

    @Test
    void industryRepresentativeCannotUseAcademicEvaluationForms() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.assertEvaluationScope(
                        actor(UserRole.INDUSTRY_REPRESENTATIVE),
                        EvaluationType.ORAL_PHASE_II,
                        phase(PhaseType.PHASE_II)
                )
        );

        assertEquals("INDUSTRY_DEMO_DAY_ONLY", exception.getErrorCode());
    }

    @Test
    void demoDayCannotBeSubmittedAgainstPhaseOne() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.assertEvaluationScope(
                        actor(UserRole.INDUSTRY_REPRESENTATIVE),
                        EvaluationType.DEMO_DAY_INDUSTRY,
                        phase(PhaseType.PHASE_I)
                )
        );

        assertEquals("EVALUATION_PHASE_MISMATCH", exception.getErrorCode());
    }

    private User actor(UserRole role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    private Phase phase(PhaseType type) {
        Phase phase = new Phase();
        phase.setType(type);
        return phase;
    }
}
