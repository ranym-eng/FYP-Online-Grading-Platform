package fyp_grading_platform.importing;

import fyp_grading_platform.audit.AuditService;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.ProjectSupervisorAssignmentRepository;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformInitializationImportServiceTest {
    private TrackRepository tracks;
    private StudentProfileRepository students;
    private UserRepository users;
    private ProjectRepository projects;
    private PlatformInitializationImportService service;

    @BeforeEach
    void setUp() {
        tracks = mock(TrackRepository.class);
        students = mock(StudentProfileRepository.class);
        users = mock(UserRepository.class);
        projects = mock(ProjectRepository.class);
        when(tracks.findAll()).thenReturn(List.of());
        when(students.findAll()).thenReturn(List.of());
        when(users.findAll()).thenReturn(List.of());
        when(projects.findAll()).thenReturn(List.of());

        service = new PlatformInitializationImportService(
                tracks,
                students,
                users,
                mock(EvaluatorProfileRepository.class),
                projects,
                mock(TeamRepository.class),
                mock(ProjectSupervisorAssignmentRepository.class),
                mock(ProjectEvaluatorAssignmentRepository.class),
                mock(PhaseRepository.class),
                mock(PasswordEncoder.class),
                mock(AuditService.class)
        );
    }

    @Test
    void acceptsACompleteConsistentWorkbook() throws Exception {
        InitializationImportReport report = service.preview(workbook("DEMO_DAY_INDUSTRY", "20270001"));

        assertThat(report.importable()).isTrue();
        assertThat(report.errors()).isEmpty();
        assertThat(report.totalRows()).isEqualTo(10);
        assertThat(report.sheets()).hasSize(8);
    }

    @Test
    void rejectsIndustryAssignmentOutsideDemoDay() throws Exception {
        InitializationImportReport report = service.preview(workbook("REPORT_PHASE_I", "20270001"));

        assertThat(report.importable()).isFalse();
        assertThat(report.errors()).anySatisfy(error -> {
            assertThat(error.sheet()).isEqualTo("EVALUATOR_ASSIGNMENTS");
            assertThat(error.field()).isEqualTo("evaluationType");
            assertThat(error.message()).contains("DEMO_DAY_INDUSTRY");
        });
    }

    @Test
    void rejectsUnknownStudentInTeamMembership() throws Exception {
        InitializationImportReport report = service.preview(workbook("DEMO_DAY_INDUSTRY", "99999999"));

        assertThat(report.importable()).isFalse();
        assertThat(report.errors()).anySatisfy(error -> {
            assertThat(error.sheet()).isEqualTo("TEAM_MEMBERS");
            assertThat(error.field()).isEqualTo("studentNumber");
            assertThat(error.message()).isEqualTo("Unknown student");
        });
    }

    private MockMultipartFile workbook(String industryEvaluationType, String teamStudentNumber) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            sheet(workbook, "TRACKS",
                    List.of("code", "name", "description", "active"),
                    List.of(List.of("PSE", "Power Systems Engineering", "Power Systems Engineering", "TRUE")));
            sheet(workbook, "STUDENTS",
                    List.of("studentNumber", "fullName", "email", "cohort", "academicYear", "trackCode", "level"),
                    List.of(List.of("20270001", "Ali Al Harthy", "s20270001@student.squ.edu.om", "2023", "2026-2027", "PSE", "Final Year")));
            sheet(workbook, "ACTORS",
                    List.of("universityId", "fullName", "email", "role", "department", "specialization", "externalOrganization", "phone", "temporaryPassword", "status"),
                    List.of(
                            List.of("SUP001", "Dr Sara", "sara@squ.edu.om", "SUPERVISOR", "Electrical Engineering", "Power", "", "", "ChangeMe@123", "ACTIVE"),
                            List.of("IND001", "Ahmed Guest", "ahmed@industry.om", "INDUSTRY_REPRESENTATIVE", "", "", "Oman Energy", "", "ChangeMe@123", "ACTIVE")
                    ));
            sheet(workbook, "PROJECTS",
                    List.of("projectNumber", "title", "academicYear", "trackCode", "status", "abstractText", "teamName", "section"),
                    List.of(List.of("PSE-01", "Smart Grid Monitoring", "2026-2027", "PSE", "ACTIVE", "Monitoring project", "Team PSE-01", "PSE-A")));
            sheet(workbook, "TEAM_MEMBERS",
                    List.of("projectNumber", "studentNumber"),
                    List.of(List.of("PSE-01", teamStudentNumber)));
            sheet(workbook, "SUPERVISORS",
                    List.of("projectNumber", "supervisorEmail"),
                    List.of(List.of("PSE-01", "sara@squ.edu.om")));
            sheet(workbook, "EVALUATOR_ASSIGNMENTS",
                    List.of("projectNumber", "evaluatorEmail", "evaluationType"),
                    List.of(List.of("PSE-01", "ahmed@industry.om", industryEvaluationType)));
            sheet(workbook, "PHASES",
                    List.of("name", "type", "academicYear", "startDate", "deadline", "status"),
                    List.of(
                            List.of("FYP I 2026-2027", "PHASE_I", "2026-2027", "2026-09-01 08:00", "2026-12-15 23:59", "NOT_STARTED"),
                            List.of("FYP II 2026-2027", "PHASE_II", "2026-2027", "2027-01-15 08:00", "2027-05-15 23:59", "NOT_STARTED")
                    ));
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "initialization.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }

    private void sheet(Workbook workbook, String name, List<String> headers, List<List<String>> values) {
        Sheet sheet = workbook.createSheet(name);
        Row header = sheet.createRow(0);
        for (int column = 0; column < headers.size(); column++) header.createCell(column).setCellValue(headers.get(column));
        for (int rowIndex = 0; rowIndex < values.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<String> rowValues = values.get(rowIndex);
            for (int column = 0; column < rowValues.size(); column++) row.createCell(column).setCellValue(rowValues.get(column));
        }
    }
}
