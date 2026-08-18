package fyp_grading_platform.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.audit.AuditLog;
import fyp_grading_platform.audit.AuditLogRepository;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.evaluation.EvaluationSheetCalculator;
import fyp_grading_platform.evaluation.EvaluationSubmission;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.grading.StudentPhaseGrade;
import fyp_grading_platform.grading.StudentPhaseGradeRepository;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.ProjectEvaluatorAssignment;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectSupervisorAssignmentRepository;
import fyp_grading_platform.project.Team;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.user.StudentProfile;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FinalResultsExportService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<EvaluationType> PHASE_I_TYPES = Set.of(
            EvaluationType.SUPERVISOR_PHASE_I,
            EvaluationType.REPORT_PHASE_I,
            EvaluationType.ORAL_PHASE_I
    );
    private static final Set<EvaluationType> PHASE_II_TYPES = Set.of(
            EvaluationType.SUPERVISOR_PHASE_II,
            EvaluationType.REPORT_PHASE_II,
            EvaluationType.ORAL_PHASE_II,
            EvaluationType.DEMO_DAY_INDUSTRY
    );

    private final TeamRepository teams;
    private final PhaseRepository phases;
    private final StudentPhaseGradeRepository grades;
    private final EvaluationSubmissionRepository submissions;
    private final ProjectEvaluatorAssignmentRepository assignments;
    private final ProjectSupervisorAssignmentRepository supervisors;
    private final AuditLogRepository auditLogs;
    private final EvaluationSheetCalculator calculator;
    private final ObjectMapper objectMapper;

    public FinalResultsExportService(
            TeamRepository teams,
            PhaseRepository phases,
            StudentPhaseGradeRepository grades,
            EvaluationSubmissionRepository submissions,
            ProjectEvaluatorAssignmentRepository assignments,
            ProjectSupervisorAssignmentRepository supervisors,
            AuditLogRepository auditLogs,
            EvaluationSheetCalculator calculator,
            ObjectMapper objectMapper
    ) {
        this.teams = teams;
        this.phases = phases;
        this.grades = grades;
        this.submissions = submissions;
        this.assignments = assignments;
        this.supervisors = supervisors;
        this.auditLogs = auditLogs;
        this.calculator = calculator;
        this.objectMapper = objectMapper;
    }

    public byte[] generatePhase(UUID phaseId) {
        Phase phase = phases.findById(phaseId)
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        List<Team> selectedTeams = teams.findAll().stream()
                .filter(team -> team.getProject().getAcademicYear().equalsIgnoreCase(phase.getAcademicYear()))
                .toList();
        return build(selectedTeams, List.of(phase));
    }

    public byte[] generateProject(UUID projectId) {
        Team team = teams.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("TEAM_NOT_FOUND", "The project has no team"));
        List<Phase> selectedPhases = phases.findByAcademicYear(team.getProject().getAcademicYear());
        return build(List.of(team), selectedPhases);
    }

    public byte[] generateProjectPhase(UUID projectId, UUID phaseId) {
        Team team = teams.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("TEAM_NOT_FOUND", "The project has no team"));
        Phase phase = phases.findById(phaseId)
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        if (!team.getProject().getAcademicYear().equalsIgnoreCase(phase.getAcademicYear())) {
            throw new BusinessException("PROJECT_PHASE_MISMATCH", "Project and phase do not belong to the same cohort");
        }
        return build(List.of(team), List.of(phase));
    }

    public List<EvaluationCompletenessRow> completeness(UUID phaseId) {
        Phase phase = phases.findById(phaseId)
                .orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        return teams.findAll().stream()
                .filter(team -> team.getProject().getAcademicYear().equalsIgnoreCase(phase.getAcademicYear()))
                .flatMap(team -> completeness(team, phase).stream())
                .sorted(Comparator.comparing(EvaluationCompletenessRow::projectNumber)
                        .thenComparing(row -> row.evaluationType().name())
                        .thenComparing(EvaluationCompletenessRow::evaluatorName))
                .toList();
    }

    private byte[] build(List<Team> selectedTeams, List<Phase> selectedPhases) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            WorkbookStyles styles = new WorkbookStyles(workbook);
            Sheet legacy = workbook.createSheet("LEGACY_SUMMARY");
            Sheet summary = workbook.createSheet("FINAL_SUMMARY");
            Sheet details = workbook.createSheet("EVALUATOR_DETAILS");
            Sheet missing = workbook.createSheet("MISSING_FORMS");
            Sheet audit = workbook.createSheet("AUDIT_TRAIL");

            writeHeader(legacy, styles, List.of(
                    "StudentName", "StudentID", "ProjectNumber", "ProjectTitle", "SupervisorName",
                    "PresentationScores", "AvgPresentationScore", "DemoScores", "AvgDemoScore",
                    "ReportScore", "SupervisorScores"
            ));
            writeHeader(summary, styles, List.of(
                    "Cohort", "Track", "Phase", "StudentID", "StudentName", "ProjectNumber",
                    "ProjectTitle", "Supervisors", "PresentationScores", "AvgPresentation",
                    "ReportScores", "AvgReport", "SupervisorScores", "AvgSupervisor",
                    "DemoScores", "AvgDemo", "PhaseFinalScore", "Published", "CalculatedAt"
            ));
            writeHeader(details, styles, List.of(
                    "Phase", "ProjectNumber", "ProjectTitle", "StudentID", "StudentName",
                    "EvaluationType", "EvaluatorName", "EvaluatorEmail", "Status",
                    "SubmittedAt", "LockedAt", "StudentScore", "Comments"
            ));
            writeHeader(missing, styles, List.of(
                    "Phase", "ProjectNumber", "ProjectTitle", "EvaluationType",
                    "EvaluatorName", "EvaluatorEmail", "Status"
            ));
            writeHeader(audit, styles, List.of(
                    "Timestamp", "UserId", "Action", "EntityType", "EntityId", "OldValue", "NewValue"
            ));

            int legacyRow = 1;
            int summaryRow = 1;
            int detailRow = 1;
            int missingRow = 1;
            for (Phase phase : selectedPhases.stream().sorted(Comparator.comparing(Phase::getType)).toList()) {
                for (Team team : selectedTeams.stream()
                        .sorted(Comparator.comparing(item -> item.getProject().getProjectNumber()))
                        .toList()) {
                    if (!team.getProject().getAcademicYear().equalsIgnoreCase(phase.getAcademicYear())) continue;
                    Map<UUID, StudentPhaseGrade> gradeByStudent = grades
                            .findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(
                                    team.getProject().getId(),
                                    phase.getId()
                            )
                            .stream()
                            .collect(Collectors.toMap(item -> item.getStudent().getId(), Function.identity()));
                    String supervisorNames = supervisors.findAllByProjectIdAndActiveTrue(team.getProject().getId()).stream()
                            .map(item -> item.getSupervisor().getUser().getFullName())
                            .distinct()
                            .collect(Collectors.joining(", "));

                    for (StudentProfile student : team.getStudents().stream()
                            .sorted(Comparator.comparing(StudentProfile::getStudentNumber))
                            .toList()) {
                        StudentPhaseGrade grade = gradeByStudent.get(student.getId());
                        ScoreCollection oral = scores(team, phase, oralType(phase.getType()), student);
                        ScoreCollection report = scores(team, phase, reportType(phase.getType()), student);
                        ScoreCollection supervisor = scores(team, phase, supervisorType(phase.getType()), student);
                        ScoreCollection demo = phase.getType() == PhaseType.PHASE_II
                                ? scores(team, phase, EvaluationType.DEMO_DAY_INDUSTRY, student)
                                : ScoreCollection.empty();

                        Row legacyData = legacy.createRow(legacyRow++);
                        cells(legacyData, styles, java.util.Arrays.asList(
                                student.getFullName(),
                                student.getStudentNumber(),
                                team.getProject().getProjectNumber(),
                                team.getProject().getTitle(),
                                supervisorNames,
                                oral.joined(),
                                oral.averageValue(),
                                demo.joined(),
                                demo.averageValue(),
                                report.averageValue(),
                                supervisor.joined()
                        ));

                        Row summaryData = summary.createRow(summaryRow++);
                        cells(summaryData, styles, java.util.Arrays.asList(
                                student.getCohort(),
                                team.getProject().getTrack().getCode(),
                                phase.getType().name(),
                                student.getStudentNumber(),
                                student.getFullName(),
                                team.getProject().getProjectNumber(),
                                team.getProject().getTitle(),
                                supervisorNames,
                                oral.joined(),
                                oral.averageValue(),
                                report.joined(),
                                report.averageValue(),
                                supervisor.joined(),
                                supervisor.averageValue(),
                                demo.joined(),
                                demo.averageValue(),
                                grade == null ? null : grade.getFinalScore(),
                                grade != null && grade.isPublished(),
                                grade == null || grade.getCalculatedAt() == null
                                        ? null
                                        : DATE_TIME.format(grade.getCalculatedAt())
                        ));

                        for (EvaluationSubmission submission : lockedSubmissions(team, phase)) {
                            Row detailData = details.createRow(detailRow++);
                            cells(detailData, styles, java.util.Arrays.asList(
                                    phase.getType().name(),
                                    team.getProject().getProjectNumber(),
                                    team.getProject().getTitle(),
                                    student.getStudentNumber(),
                                    student.getFullName(),
                                    submission.getEvaluationType().name(),
                                    submission.getEvaluator().getUser().getFullName(),
                                    submission.getEvaluator().getUser().getEmail(),
                                    submission.getStatus().name(),
                                    format(submission.getSubmittedAt()),
                                    format(submission.getLockedAt()),
                                    score(submission, student),
                                    submission.getGeneralComment()
                            ));
                        }
                    }

                    for (EvaluationCompletenessRow row : completeness(team, phase)) {
                        if ("LOCKED".equals(row.status())) continue;
                        Row missingData = missing.createRow(missingRow++);
                        cells(missingData, styles, java.util.Arrays.asList(
                                phase.getType().name(),
                                row.projectNumber(),
                                row.projectTitle(),
                                row.evaluationType().name(),
                                row.evaluatorName(),
                                row.evaluatorEmail(),
                                row.status()
                        ));
                    }
                }
            }

            Set<UUID> auditScope = new HashSet<>();
            selectedPhases.forEach(phase -> auditScope.add(phase.getId()));
            for (Team team : selectedTeams) {
                auditScope.add(team.getId());
                auditScope.add(team.getProject().getId());
                team.getStudents().forEach(student -> auditScope.add(student.getId()));
                for (Phase phase : selectedPhases) {
                    submissions.findByProjectIdAndPhaseId(team.getProject().getId(), phase.getId())
                            .forEach(submission -> auditScope.add(submission.getId()));
                    grades.findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(team.getProject().getId(), phase.getId())
                            .forEach(grade -> auditScope.add(grade.getId()));
                }
            }

            int auditRow = 1;
            for (AuditLog log : auditLogs.findAll().stream()
                    .filter(log -> log.getEntityId() != null && auditScope.contains(log.getEntityId()))
                    .sorted(Comparator.comparing(AuditLog::getCreatedAt))
                    .toList()) {
                Row data = audit.createRow(auditRow++);
                cells(data, styles, java.util.Arrays.asList(
                        format(log.getCreatedAt()),
                        log.getUserId(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getOldValue(),
                        log.getNewValue()
                ));
            }

            for (Sheet sheet : List.of(legacy, summary, details, missing, audit)) {
                finish(sheet);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("EXPORT_FAILED", "The Excel result file could not be generated");
        }
    }

    private List<EvaluationCompletenessRow> completeness(Team team, Phase phase) {
        Set<EvaluationType> allowed = phase.getType() == PhaseType.PHASE_I ? PHASE_I_TYPES : PHASE_II_TYPES;
        List<EvaluationSubmission> projectSubmissions = submissions
                .findByProjectIdAndPhaseId(team.getProject().getId(), phase.getId());
        return assignments.findByProjectIdAndActiveTrue(team.getProject().getId()).stream()
                .filter(assignment -> allowed.contains(assignment.getEvaluationType()))
                .map(assignment -> {
                    List<EvaluationSubmission> matching = projectSubmissions.stream()
                            .filter(item -> item.getEvaluationType() == assignment.getEvaluationType())
                            .filter(item -> item.getEvaluator().getId().equals(assignment.getEvaluator().getId()))
                            .toList();
                    String status = matching.stream().anyMatch(EvaluationSubmission::isLocked)
                            ? "LOCKED"
                            : matching.isEmpty() ? "NOT_STARTED" : "DRAFT_NOT_SUBMITTED";
                    return new EvaluationCompletenessRow(
                            team.getProject().getId(),
                            team.getProject().getProjectNumber(),
                            team.getProject().getTitle(),
                            assignment.getEvaluationType(),
                            assignment.getEvaluator().getUser().getFullName(),
                            assignment.getEvaluator().getUser().getEmail(),
                            status
                    );
                })
                .toList();
    }

    private ScoreCollection scores(Team team, Phase phase, EvaluationType type, StudentProfile student) {
        List<Double> values = lockedSubmissions(team, phase).stream()
                .filter(item -> item.getEvaluationType() == type)
                .map(item -> score(item, student))
                .toList();
        return new ScoreCollection(values);
    }

    private List<EvaluationSubmission> lockedSubmissions(Team team, Phase phase) {
        return submissions.findByProjectIdAndPhaseId(team.getProject().getId(), phase.getId()).stream()
                .filter(EvaluationSubmission::isLocked)
                .sorted(Comparator.comparing(item -> item.getEvaluator().getUser().getFullName()))
                .toList();
    }

    private double score(EvaluationSubmission submission, StudentProfile student) {
        if (submission.getScorePayload() == null || submission.getScorePayload().isBlank()) {
            return round(submission.getTotalScore());
        }
        try {
            Map<String, Double> payload = objectMapper.readValue(
                    submission.getScorePayload(),
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Double.class)
            );
            return round(calculator.calculateForTarget(
                    submission.getEvaluationType(),
                    payload,
                    student.getId().toString()
            ));
        } catch (JsonProcessingException exception) {
            throw new BusinessException("INVALID_SCORE_PAYLOAD", "A locked evaluation sheet cannot be exported");
        }
    }

    private EvaluationType oralType(PhaseType type) {
        return type == PhaseType.PHASE_I ? EvaluationType.ORAL_PHASE_I : EvaluationType.ORAL_PHASE_II;
    }

    private EvaluationType reportType(PhaseType type) {
        return type == PhaseType.PHASE_I ? EvaluationType.REPORT_PHASE_I : EvaluationType.REPORT_PHASE_II;
    }

    private EvaluationType supervisorType(PhaseType type) {
        return type == PhaseType.PHASE_I
                ? EvaluationType.SUPERVISOR_PHASE_I
                : EvaluationType.SUPERVISOR_PHASE_II;
    }

    private void writeHeader(Sheet sheet, WorkbookStyles styles, List<String> headers) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(28);
        for (int index = 0; index < headers.size(); index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(headers.get(index));
            cell.setCellStyle(styles.header());
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.size() - 1));
    }

    private void cells(Row row, WorkbookStyles styles, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            Cell cell = row.createCell(index);
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
                cell.setCellStyle(styles.number());
            } else if (value instanceof Boolean booleanValue) {
                cell.setCellValue(booleanValue);
                cell.setCellStyle(styles.body());
            } else {
                cell.setCellValue(value == null ? "" : String.valueOf(value));
                cell.setCellStyle(styles.body());
            }
        }
    }

    private void finish(Sheet sheet) {
        if (sheet.getRow(0) == null) return;
        for (int index = 0; index < sheet.getRow(0).getLastCellNum(); index++) {
            sheet.autoSizeColumn(index);
            sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 14000));
        }
    }

    private String format(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ScoreCollection(List<Double> values) {
        static ScoreCollection empty() {
            return new ScoreCollection(List.of());
        }

        String joined() {
            return values.stream()
                    .map(value -> String.format(Locale.ROOT, "%.2f", value))
                    .collect(Collectors.joining(", "));
        }

        Object averageValue() {
            if (values.isEmpty()) return "";
            return Math.round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100.0) / 100.0;
        }
    }

    private record WorkbookStyles(CellStyle header, CellStyle body, CellStyle number) {
        WorkbookStyles(XSSFWorkbook workbook) {
            this(headerStyle(workbook), bodyStyle(workbook), numberStyle(workbook));
        }

        private static CellStyle headerStyle(XSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            borders(style);
            return style;
        }

        private static CellStyle bodyStyle(XSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setVerticalAlignment(VerticalAlignment.TOP);
            style.setWrapText(true);
            borders(style);
            return style;
        }

        private static CellStyle numberStyle(XSSFWorkbook workbook) {
            CellStyle style = bodyStyle(workbook);
            style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            return style;
        }

        private static void borders(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}
