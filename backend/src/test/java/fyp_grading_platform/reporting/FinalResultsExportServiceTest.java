package fyp_grading_platform.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.audit.AuditLogRepository;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.evaluation.EvaluationSheetCalculator;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.grading.StudentPhaseGradeRepository;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectSupervisorAssignmentRepository;
import fyp_grading_platform.project.Team;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.project.Track;
import fyp_grading_platform.user.StudentProfile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalResultsExportServiceTest {
    @Mock TeamRepository teams;
    @Mock PhaseRepository phases;
    @Mock StudentPhaseGradeRepository grades;
    @Mock EvaluationSubmissionRepository submissions;
    @Mock ProjectEvaluatorAssignmentRepository assignments;
    @Mock ProjectSupervisorAssignmentRepository supervisors;
    @Mock AuditLogRepository auditLogs;

    @Test
    void producesTheFiveOperationalSheetsForOneProjectPhase() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID phaseId = UUID.randomUUID();
        Track track = new Track();
        track.setCode("PSE");
        Project project = new Project();
        project.setId(projectId);
        project.setProjectNumber("PSE-01");
        project.setTitle("Smart Grid");
        project.setAcademicYear("2026");
        project.setTrack(track);
        StudentProfile student = new StudentProfile();
        student.setId(UUID.randomUUID());
        student.setStudentNumber("20260001");
        student.setFullName("Demo Student");
        student.setCohort("2026");
        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setProject(project);
        team.getStudents().add(student);
        Phase phase = new Phase();
        phase.setId(phaseId);
        phase.setType(PhaseType.PHASE_I);
        phase.setAcademicYear("2026");
        phase.setStartDate(LocalDateTime.now().minusDays(1));
        phase.setDeadline(LocalDateTime.now().plusDays(1));

        when(teams.findByProjectId(projectId)).thenReturn(Optional.of(team));
        when(phases.findById(phaseId)).thenReturn(Optional.of(phase));
        when(grades.findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(projectId, phaseId)).thenReturn(List.of());
        when(submissions.findByProjectIdAndPhaseId(projectId, phaseId)).thenReturn(List.of());
        when(assignments.findByProjectIdAndActiveTrue(projectId)).thenReturn(List.of());
        when(supervisors.findAllByProjectIdAndActiveTrue(projectId)).thenReturn(List.of());
        when(auditLogs.findAll()).thenReturn(List.of());

        FinalResultsExportService service = new FinalResultsExportService(
                teams, phases, grades, submissions, assignments, supervisors, auditLogs,
                new EvaluationSheetCalculator(), new ObjectMapper()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(
                service.generateProjectPhase(projectId, phaseId)))) {
            assertNotNull(workbook.getSheet("LEGACY_SUMMARY"));
            assertNotNull(workbook.getSheet("FINAL_SUMMARY"));
            assertNotNull(workbook.getSheet("EVALUATOR_DETAILS"));
            assertNotNull(workbook.getSheet("MISSING_FORMS"));
            assertNotNull(workbook.getSheet("AUDIT_TRAIL"));
            assertEquals("20260001", workbook.getSheet("LEGACY_SUMMARY").getRow(1).getCell(1).getStringCellValue());
        }
    }
}