package fyp_grading_platform.grading;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.evaluation.EvaluationSheetCalculator;
import fyp_grading_platform.evaluation.EvaluationSubmission;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.Team;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.user.StudentProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsolidationServiceTest {
    @Mock StudentPhaseGradeRepository studentGrades;
    @Mock GradeRepository projectGrades;
    @Mock GradeRuleRepository rules;
    @Mock EvaluationSubmissionRepository submissions;
    @Mock ProjectRepository projects;
    @Mock PhaseRepository phases;
    @Mock TeamRepository teams;

    @Test
    void averagesEveryLockedEvaluatorWhileKeepingStudentSpecificScores() throws Exception {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTitle("Smart Grid");
        Phase phase = new Phase();
        phase.setId(UUID.randomUUID());
        phase.setType(PhaseType.PHASE_I);

        StudentProfile first = student("10001", "First Student");
        StudentProfile second = student("10002", "Second Student");
        Team team = new Team();
        team.getStudents().add(first);
        team.getStudents().add(second);

        GradeRule oralRule = new GradeRule();
        oralRule.setPhaseType(PhaseType.PHASE_I);
        oralRule.setEvaluationType(EvaluationType.ORAL_PHASE_I);
        oralRule.setWeight(100);

        EvaluationSubmission evaluatorOne = submission(project, phase, payload(first, second, 10, 6, 8));
        EvaluationSubmission evaluatorTwo = submission(project, phase, payload(first, second, 8, 4, 6));

        when(projects.findById(project.getId())).thenReturn(Optional.of(project));
        when(phases.findById(phase.getId())).thenReturn(Optional.of(phase));
        when(teams.findByProjectId(project.getId())).thenReturn(Optional.of(team));
        when(rules.findByPhaseTypeAndActiveTrue(PhaseType.PHASE_I)).thenReturn(List.of(oralRule));
        when(submissions.findByProjectIdAndPhaseId(project.getId(), phase.getId()))
                .thenReturn(List.of(evaluatorOne, evaluatorTwo));
        when(studentGrades.findByProjectIdAndPhaseIdAndStudentId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(projectGrades.findByProjectIdAndPhaseId(project.getId(), phase.getId()))
                .thenReturn(Optional.empty());

        List<StudentPhaseGrade> stored = new ArrayList<>();
        when(studentGrades.findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(project.getId(), phase.getId()))
                .thenAnswer(invocation -> List.copyOf(stored));
        when(studentGrades.saveAll(any())).thenAnswer(invocation -> {
            stored.clear();
            stored.addAll(invocation.getArgument(0));
            return stored;
        });

        ConsolidationService service = new ConsolidationService(
                studentGrades,
                projectGrades,
                rules,
                submissions,
                projects,
                phases,
                teams,
                new EvaluationSheetCalculator(),
                new ObjectMapper()
        );

        List<StudentPhaseGrade> result = service.calculate(project.getId(), phase.getId());

        assertEquals(2, result.size());
        assertEquals(7.75, result.get(0).getOralScore());
        assertEquals(7.75, result.get(0).getFinalScore());
        assertEquals(6.25, result.get(1).getOralScore());
        assertEquals(6.25, result.get(1).getFinalScore());

        ArgumentCaptor<Grade> summary = ArgumentCaptor.forClass(Grade.class);
        org.mockito.Mockito.verify(projectGrades).save(summary.capture());
        assertEquals(7.0, summary.getValue().getFinalScore());
    }

    private StudentProfile student(String number, String name) {
        StudentProfile student = new StudentProfile();
        student.setId(UUID.randomUUID());
        student.setStudentNumber(number);
        student.setFullName(name);
        return student;
    }

    private EvaluationSubmission submission(Project project, Phase phase, Map<String, Double> scores) throws Exception {
        EvaluationSubmission submission = new EvaluationSubmission();
        submission.setProject(project);
        submission.setPhase(phase);
        submission.setEvaluationType(EvaluationType.ORAL_PHASE_I);
        submission.setLocked(true);
        submission.setScorePayload(new ObjectMapper().writeValueAsString(scores));
        return submission;
    }

    private Map<String, Double> payload(
            StudentProfile first,
            StudentProfile second,
            double firstIndividual,
            double secondIndividual,
            double group
    ) {
        Map<String, Double> scores = new LinkedHashMap<>();
        individual(scores, first.getId(), firstIndividual);
        individual(scores, second.getId(), secondIndividual);
        for (String criterion : List.of(
                "technical-presentation",
                "identify-problem",
                "formulate-problem",
                "design-requirements",
                "analyze-solutions",
                "evaluate-ethics-impact",
                "complete-work"
        )) {
            scores.put("group:" + criterion + ":group", group);
        }
        return scores;
    }

    private void individual(Map<String, Double> scores, UUID studentId, double value) {
        scores.put("individual:present-information:" + studentId, value);
        scores.put("individual:answer-questions:" + studentId, value);
    }
}
