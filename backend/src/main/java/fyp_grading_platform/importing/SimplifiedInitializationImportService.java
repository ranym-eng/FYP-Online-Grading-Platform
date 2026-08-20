package fyp_grading_platform.importing;

import fyp_grading_platform.audit.AuditService;
import fyp_grading_platform.auth.IndustryInvitationService;
import fyp_grading_platform.auth.OneTimeTokenHasher;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectEvaluatorAssignment;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.ProjectSupervisorAssignment;
import fyp_grading_platform.project.ProjectSupervisorAssignmentRepository;
import fyp_grading_platform.project.Team;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.user.EvaluatorProfile;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.StudentController;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SimplifiedInitializationImportService {
    private static final long MAX_FILE_BYTES = 15L * 1024 * 1024;
    private static final List<String> SHEETS = List.of(
            "STUDENTS", "ADMINISTRATORS", "COORDINATORS", "SUPERVISORS",
            "FACULTY_EVALUATORS", "INDUSTRY_GUESTS", "PROJECT_ASSIGNMENTS"
    );
    private static final Map<String, UserRole> ACTOR_ROLES = Map.of(
            "ADMINISTRATORS", UserRole.ADMIN,
            "COORDINATORS", UserRole.COORDINATOR,
            "SUPERVISORS", UserRole.SUPERVISOR,
            "FACULTY_EVALUATORS", UserRole.FACULTY_EVALUATOR,
            "INDUSTRY_GUESTS", UserRole.INDUSTRY_REPRESENTATIVE
    );
    private static final List<String> PROJECT_METADATA = List.of(
            "cohort", "trackCode", "projectNumber", "projectTitle", "projectAbstract", "section",
            "reportPhaseIEvaluatorEmails", "oralPhaseIEvaluatorEmails",
            "reportPhaseIIEvaluatorEmails", "oralPhaseIIEvaluatorEmails", "industryGuestEmails"
    );

    private final StudentProfileRepository students;
    private final UserRepository users;
    private final EvaluatorProfileRepository evaluatorProfiles;
    private final TrackRepository tracks;
    private final ProjectRepository projects;
    private final TeamRepository teams;
    private final ProjectSupervisorAssignmentRepository supervisorAssignments;
    private final ProjectEvaluatorAssignmentRepository evaluatorAssignments;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenHasher tokenHasher;
    private final IndustryInvitationService industryInvitations;
    private final AuditService audit;

    public SimplifiedInitializationImportService(
            StudentProfileRepository students,
            UserRepository users,
            EvaluatorProfileRepository evaluatorProfiles,
            TrackRepository tracks,
            ProjectRepository projects,
            TeamRepository teams,
            ProjectSupervisorAssignmentRepository supervisorAssignments,
            ProjectEvaluatorAssignmentRepository evaluatorAssignments,
            PasswordEncoder passwordEncoder,
            OneTimeTokenHasher tokenHasher,
            IndustryInvitationService industryInvitations,
            AuditService audit
    ) {
        this.students = students;
        this.users = users;
        this.evaluatorProfiles = evaluatorProfiles;
        this.tracks = tracks;
        this.projects = projects;
        this.teams = teams;
        this.supervisorAssignments = supervisorAssignments;
        this.evaluatorAssignments = evaluatorAssignments;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.industryInvitations = industryInvitations;
        this.audit = audit;
    }

    public boolean supports(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            return workbook.getSheet("PROJECT_ASSIGNMENTS") != null;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public InitializationImportReport preview(MultipartFile file) {
        Parsed parsed = parse(file);
        return validate(parsed, true);
    }

    @Transactional
    public InitializationImportReport importWorkbook(MultipartFile file, User actor) {
        Parsed parsed = parse(file);
        InitializationImportReport validation = validate(parsed, false);
        if (!validation.importable()) return validation;
        Map<String, Counter> counters = SHEETS.stream().collect(Collectors.toMap(
                Function.identity(), ignored -> new Counter(), (a, b) -> a, LinkedHashMap::new
        ));
        importStudents(parsed.rows("STUDENTS"), counters.get("STUDENTS"));
        Map<String, User> actorsById = importActors(parsed, counters);
        importProjectsAndAssignments(parsed.rows("PROJECT_ASSIGNMENTS"), actorsById, counters.get("PROJECT_ASSIGNMENTS"));
        List<InitializationSheetSummary> summaries = SHEETS.stream()
                .map(sheet -> new InitializationSheetSummary(
                        sheet, parsed.rows(sheet).size(), parsed.rows(sheet).size(),
                        counters.get(sheet).created, counters.get(sheet).updated, counters.get(sheet).unchanged
                ))
                .toList();
        int total = summaries.stream().mapToInt(InitializationSheetSummary::totalRows).sum();
        audit.record(actor.getId(), "PLATFORM_INITIALIZED_FROM_SIMPLIFIED_WORKBOOK", "PlatformData",
                actor.getId(), null, "file=" + safeFilename(file) + ", rows=" + total);
        return new InitializationImportReport(false, true, total, total, summaries, List.of());
    }

    private InitializationImportReport validate(Parsed parsed, boolean preview) {
        List<InitializationImportError> errors = new ArrayList<>(parsed.errors());
        Map<String, RowData> studentsById = uniqueIndex(parsed.rows("STUDENTS"), "studentId", errors);
        uniqueIndex(parsed.rows("STUDENTS"), "email", errors);
        Map<String, RowData> actorsById = new LinkedHashMap<>();
        Map<String, RowData> actorsByEmail = new LinkedHashMap<>();
        validateStudents(parsed.rows("STUDENTS"), errors);
        for (Map.Entry<String, UserRole> entry : ACTOR_ROLES.entrySet()) {
            validateActors(parsed.rows(entry.getKey()), entry.getValue(), actorsById, actorsByEmail, errors);
        }
        validateProjects(parsed.rows("PROJECT_ASSIGNMENTS"), studentsById, actorsById, actorsByEmail, errors);

        Set<RowLocation> invalid = errors.stream()
                .filter(error -> error.rowNumber() > 0)
                .map(error -> new RowLocation(error.sheet(), error.rowNumber()))
                .collect(Collectors.toSet());
        List<InitializationSheetSummary> summaries = SHEETS.stream().map(sheet -> {
            int total = parsed.rows(sheet).size();
            int invalidCount = (int) invalid.stream().filter(row -> row.sheet.equals(sheet)).count();
            return new InitializationSheetSummary(sheet, total, Math.max(0, total - invalidCount), 0, 0, 0);
        }).toList();        int total = summaries.stream().mapToInt(InitializationSheetSummary::totalRows).sum();
        int valid = summaries.stream().mapToInt(InitializationSheetSummary::validRows).sum();
        return new InitializationImportReport(preview, errors.isEmpty(), total, valid, summaries, List.copyOf(errors));
    }

    private void validateStudents(List<RowData> rows, List<InitializationImportError> errors) {
        Set<String> knownTracks = tracks.findAll().stream()
                .map(track -> upper(track.getCode())).collect(Collectors.toSet());
        for (RowData row : rows) {
            for (String field : List.of("studentId", "studentName", "email", "cohort", "trackCode", "level")) {
                required(row, field, errors);
            }
            String id = StudentController.normalizeStudentNumber(row.value("studentId"));
            String email = lower(row.value("email"));
            if (!id.isBlank() && !id.matches("\\d{5,12}")) {
                error(row, "studentId", "Student ID must contain 5 to 12 digits", errors);
            }
            if (!id.isBlank() && !email.equals("s" + id + "@student.squ.edu.om")) {
                error(row, "email", "Expected SQU email: s" + id + "@student.squ.edu.om", errors);
            }
            if (!row.value("cohort").matches("(?:19|20)\\d{2}")) {
                error(row, "cohort", "Cohort must use YYYY format", errors);
            }
            if (!knownTracks.contains(upper(row.value("trackCode")))) {
                error(row, "trackCode", "Unknown track code", errors);
            }
        }
    }

    private void validateActors(
            List<RowData> rows,
            UserRole role,
            Map<String, RowData> actorsById,
            Map<String, RowData> actorsByEmail,
            List<InitializationImportError> errors
    ) {
        for (RowData row : rows) {
            for (String field : List.of("actorId", "actorName", "email", "status")) required(row, field, errors);
            String id = lower(row.value("actorId"));
            String email = lower(row.value("email"));
            duplicate(row, "actorId", id, actorsById, "Actor ID is duplicated across role sheets", errors);
            duplicate(row, "email", email, actorsByEmail, "Actor email is duplicated across role sheets", errors);
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                error(row, "email", "Invalid email address", errors);
            }
            if (role != UserRole.INDUSTRY_REPRESENTATIVE && !email.endsWith("@squ.edu.om")) {
                error(row, "email", "Internal actors require an institutional @squ.edu.om email", errors);
            }
            if (role == UserRole.INDUSTRY_REPRESENTATIVE && row.value("organization").isBlank()) {
                error(row, "organization", "Industry guests require an organization", errors);
            }
            if (role == UserRole.INDUSTRY_REPRESENTATIVE) {
                if (!"PENDING_INVITATION".equals(upper(row.value("status")))) {
                    error(row, "status", "New Industry Guests must use PENDING_INVITATION", errors);
                }
                if (row.value("accessExpiresAt").isBlank()) {
                    error(row, "accessExpiresAt", "Industry Guest access requires an expiration date", errors);
                } else {
                    try {
                        if (parseAccessExpiresAt(row.value("accessExpiresAt")).isBefore(LocalDateTime.now())) {
                            error(row, "accessExpiresAt", "Industry Guest access expiration must be in the future", errors);
                        }
                    } catch (IllegalArgumentException exception) {
                        error(row, "accessExpiresAt", "Use YYYY-MM-DD or YYYY-MM-DDTHH:mm", errors);
                    }
                }
            }
            try {
                UserStatus.valueOf(upper(row.value("status")));
            } catch (IllegalArgumentException exception) {
                error(row, "status", "Status must be ACTIVE, PENDING_INVITATION, INACTIVE or SUSPENDED", errors);
            }
        }
    }

    private void validateProjects(
            List<RowData> rows,
            Map<String, RowData> studentsById,
            Map<String, RowData> actorsById,
            Map<String, RowData> actorsByEmail,
            List<InitializationImportError> errors
    ) {
        Map<String, List<RowData>> grouped = rows.stream()
                .filter(row -> !row.value("projectNumber").isBlank())
                .collect(Collectors.groupingBy(row -> upper(row.value("projectNumber")),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, String> studentProject = new HashMap<>();
        for (Map.Entry<String, List<RowData>> entry : grouped.entrySet()) {
            List<RowData> projectRows = entry.getValue();
            RowData first = projectRows.get(0);
            for (String field : List.of("cohort", "trackCode", "projectNumber", "projectTitle")) {
                required(first, field, errors);
            }
            if (tracks.findByCode(upper(first.value("trackCode"))).isEmpty()) {
                error(first, "trackCode", "Unknown track code", errors);
            }
            Set<String> projectStudents = new LinkedHashSet<>();
            Set<String> projectSupervisors = new LinkedHashSet<>();
            for (RowData row : projectRows) {
                String studentId = StudentController.normalizeStudentNumber(row.value("studentId"));
                if (!studentId.isBlank()) {
                    if (!studentsById.containsKey(lower(studentId))) {
                        error(row, "studentId", "Student is missing from STUDENTS", errors);
                    }
                    if (!projectStudents.add(lower(studentId))) {
                        error(row, "studentId", "Student is duplicated in this project", errors);
                    }
                    String key = lower(first.value("cohort")) + ":" + lower(studentId);
                    String previous = studentProject.putIfAbsent(key, entry.getKey());
                    if (previous != null && !previous.equals(entry.getKey())) {
                        error(row, "studentId", "Student belongs to another project in the same cohort", errors);
                    }
                }
                String supervisorId = lower(row.value("supervisorId"));
                if (!supervisorId.isBlank()) {
                    RowData actor = actorsById.get(supervisorId);
                    if (actor == null || !"SUPERVISORS".equals(actor.sheet())) {
                        error(row, "supervisorId", "Supervisor is missing from SUPERVISORS", errors);
                    }
                    projectSupervisors.add(supervisorId);
                }
            }
            if (projectStudents.isEmpty()) error(first, "studentId", "A project requires at least one student", errors);
            if (projectStudents.size() > 5) error(first, "studentId", "A project cannot contain more than 5 students", errors);
            if (projectSupervisors.isEmpty()) error(first, "supervisorId", "A project requires at least one supervisor", errors);
            if (projectSupervisors.size() > 2) error(first, "supervisorId", "A project cannot contain more than 2 supervisors", errors);
            validateEvaluatorEmails(first, "reportPhaseIEvaluatorEmails", UserRole.FACULTY_EVALUATOR, actorsByEmail, errors);
            validateEvaluatorEmails(first, "oralPhaseIEvaluatorEmails", UserRole.FACULTY_EVALUATOR, actorsByEmail, errors);
            validateEvaluatorEmails(first, "reportPhaseIIEvaluatorEmails", UserRole.FACULTY_EVALUATOR, actorsByEmail, errors);
            validateEvaluatorEmails(first, "oralPhaseIIEvaluatorEmails", UserRole.FACULTY_EVALUATOR, actorsByEmail, errors);
            validateEvaluatorEmails(first, "industryGuestEmails", UserRole.INDUSTRY_REPRESENTATIVE, actorsByEmail, errors);
        }
    }

    private void validateEvaluatorEmails(
            RowData row,
            String field,
            UserRole requiredRole,
            Map<String, RowData> actorsByEmail,
            List<InitializationImportError> errors
    ) {
        for (String email : splitEmails(row.value(field))) {
            RowData actor = actorsByEmail.get(lower(email));
            if (actor == null) error(row, field, "Evaluator is missing from actor sheets: " + email, errors);
            else if (ACTOR_ROLES.get(actor.sheet()) != requiredRole) {
                error(row, field, "Evaluator has the wrong role: " + email, errors);
            }
        }
    }

    private void importStudents(List<RowData> rows, Counter counter) {
        for (RowData row : rows) {
            String number = StudentController.normalizeStudentNumber(row.value("studentId"));
            StudentProfile student = students.findByStudentNumber(number).orElse(null);            boolean created = student == null;
            if (created) student = new StudentProfile();
            String name = StudentController.normalizeName(row.value("studentName"));
            String email = lower(row.value("email"));
            String cohort = row.value("cohort");
            String trackCode = upper(row.value("trackCode"));
            String level = row.value("level");
            boolean changed = created || different(student.getFullName(), name)
                    || different(student.getEmail(), email) || different(student.getCohort(), cohort)
                    || different(student.getTrackCode(), trackCode) || different(student.getLevel(), level);
            student.setStudentNumber(number);
            student.setFullName(name);
            student.setEmail(email);
            student.setCohort(cohort);
            student.setAcademicYear(cohort);
            student.setTrackCode(trackCode);
            student.setLevel(level);
            students.save(student);
            counter.record(created, changed);
        }
    }

    private Map<String, User> importActors(Parsed parsed, Map<String, Counter> counters) {
        Map<String, User> result = new LinkedHashMap<>();
        for (Map.Entry<String, UserRole> entry : ACTOR_ROLES.entrySet()) {
            for (RowData row : parsed.rows(entry.getKey())) {
                String email = lower(row.value("email"));
                User user = users.findByEmailIgnoreCase(email)
                        .or(() -> users.findByUniversityId(row.value("actorId"))).orElse(null);
                boolean created = user == null;
                if (created) user = new User();
                UserStatus status = UserStatus.valueOf(upper(row.value("status")));
                boolean changed = created || different(user.getUniversityId(), row.value("actorId"))
                        || different(user.getFullName(), row.value("actorName"))
                        || different(user.getEmail(), email) || user.getRole() != entry.getValue()
                        || user.getStatus() != status
                        || different(user.getAccessExpiresAt(), entry.getValue() == UserRole.INDUSTRY_REPRESENTATIVE
                                ? parseAccessExpiresAt(row.value("accessExpiresAt"))
                                : null);
                user.setUniversityId(row.value("actorId"));
                user.setFullName(row.value("actorName"));
                user.setEmail(email);
                user.setPhone(blankToNull(row.value("phone")));
                user.setRole(entry.getValue());
                user.setStatus(status);
                user.setAccessExpiresAt(entry.getValue() == UserRole.INDUSTRY_REPRESENTATIVE
                        ? parseAccessExpiresAt(row.value("accessExpiresAt"))
                        : null);
                if (created) {
                    user.setPasswordHash(passwordEncoder.encode(tokenHasher.generate()));
                }
                user = users.save(user);
                if (isEvaluator(entry.getValue())) upsertEvaluatorProfile(user, row, entry.getValue());
                if (entry.getValue() == UserRole.INDUSTRY_REPRESENTATIVE
                        && status == UserStatus.PENDING_INVITATION) {
                    industryInvitations.invite(user);
                }
                result.put(lower(row.value("actorId")), user);
                counters.get(entry.getKey()).record(created, changed);
            }
        }
        return result;
    }

    private void upsertEvaluatorProfile(User user, RowData row, UserRole role) {
        EvaluatorProfile profile = evaluatorProfiles.findByUserId(user.getId()).orElseGet(EvaluatorProfile::new);
        profile.setUser(user);
        profile.setDepartment(blankToNull(row.value("department")));
        profile.setSpecialization(blankToNull(row.value("specialization")));
        profile.setExternalOrganization(blankToNull(row.value("organization")));
        profile.setExternal(role == UserRole.INDUSTRY_REPRESENTATIVE);
        evaluatorProfiles.save(profile);
    }

    private void importProjectsAndAssignments(List<RowData> rows, Map<String, User> actorsById, Counter counter) {
        Map<String, List<RowData>> grouped = rows.stream().collect(Collectors.groupingBy(
                row -> upper(row.value("projectNumber")), LinkedHashMap::new, Collectors.toList()));
        for (List<RowData> projectRows : grouped.values()) {
            RowData first = projectRows.get(0);
            String number = upper(first.value("projectNumber"));
            Project project = projects.findByProjectNumberIgnoreCase(number).orElse(null);
            boolean created = project == null;
            if (created) project = new Project();
            project.setProjectNumber(number);
            project.setTitle(first.value("projectTitle"));
            project.setAbstractText(blankToNull(first.value("projectAbstract")));
            project.setAcademicYear(first.value("cohort"));
            project.setStatus("ACTIVE");
            project.setTrack(tracks.findByCode(upper(first.value("trackCode"))).orElseThrow());
            project = projects.save(project);

            Team team = teams.findByProjectId(project.getId()).orElseGet(Team::new);
            team.setProject(project);
            team.setName(number + " Team");
            team.setSection(blankToNull(first.value("section")));
            team.setAcademicYear(first.value("cohort"));
            team.getStudents().clear();
            for (RowData row : projectRows) {
                if (!row.value("studentId").isBlank()) {
                    team.getStudents().add(students.findByStudentNumber(
                            StudentController.normalizeStudentNumber(row.value("studentId"))).orElseThrow());
                }
            }
            teams.save(team);

            supervisorAssignments.findAllByProjectIdAndActiveTrue(project.getId())
                    .forEach(assignment -> assignment.setActive(false));
            evaluatorAssignments.findByProjectIdAndActiveTrue(project.getId())
                    .forEach(assignment -> assignment.setActive(false));

            Set<String> supervisorIds = projectRows.stream().map(row -> lower(row.value("supervisorId")))
                    .filter(value -> !value.isBlank()).collect(Collectors.toCollection(LinkedHashSet::new));
            for (String supervisorId : supervisorIds) {
                User user = actorsById.get(supervisorId);
                EvaluatorProfile evaluator = evaluatorProfiles.findByUserId(user.getId()).orElseThrow();
                upsertSupervisor(project, evaluator);
                upsertEvaluator(project, evaluator, EvaluationType.SUPERVISOR_PHASE_I);
                upsertEvaluator(project, evaluator, EvaluationType.SUPERVISOR_PHASE_II);
            }
            assignEmails(project, first.value("reportPhaseIEvaluatorEmails"), EvaluationType.REPORT_PHASE_I);
            assignEmails(project, first.value("oralPhaseIEvaluatorEmails"), EvaluationType.ORAL_PHASE_I);
            assignEmails(project, first.value("reportPhaseIIEvaluatorEmails"), EvaluationType.REPORT_PHASE_II);
            assignEmails(project, first.value("oralPhaseIIEvaluatorEmails"), EvaluationType.ORAL_PHASE_II);
            assignEmails(project, first.value("industryGuestEmails"), EvaluationType.DEMO_DAY_INDUSTRY);
            counter.record(created, true);
        }
    }

    private void upsertSupervisor(Project project, EvaluatorProfile evaluator) {
        ProjectSupervisorAssignment assignment = supervisorAssignments
                .findByProjectIdAndSupervisorId(project.getId(), evaluator.getId())
                .orElseGet(ProjectSupervisorAssignment::new);
        assignment.setProject(project);
        assignment.setSupervisor(evaluator);
        assignment.setActive(true);
        supervisorAssignments.save(assignment);
    }

    private void assignEmails(Project project, String value, EvaluationType type) {
        for (String email : splitEmails(value)) {
            User user = users.findByEmailIgnoreCase(email).orElseThrow();
            EvaluatorProfile evaluator = evaluatorProfiles.findByUserId(user.getId()).orElseThrow();
            upsertEvaluator(project, evaluator, type);
        }
    }

    private void upsertEvaluator(Project project, EvaluatorProfile evaluator, EvaluationType type) {
        ProjectEvaluatorAssignment assignment = evaluatorAssignments                .findByProjectIdAndEvaluatorIdAndEvaluationType(project.getId(), evaluator.getId(), type)
                .orElseGet(ProjectEvaluatorAssignment::new);
        assignment.setProject(project);
        assignment.setEvaluator(evaluator);
        assignment.setEvaluationType(type);
        assignment.setActive(true);
        evaluatorAssignments.save(assignment);
    }

    private Parsed parse(MultipartFile file) {
        validateFile(file);
        List<InitializationImportError> errors = new ArrayList<>();
        Map<String, List<RowData>> values = new LinkedHashMap<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            for (String name : SHEETS) {
                Sheet sheet = workbook.getSheet(name);
                if (sheet == null) {
                    errors.add(new InitializationImportError(name, 0, "sheet", "", "Required sheet is missing"));
                    values.put(name, List.of());
                } else {
                    values.put(name, readSheet(sheet, name, errors));
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException("INVALID_EXCEL_FILE", "The initialization workbook could not be read");
        }
        return new Parsed(values, errors);
    }

    private List<RowData> readSheet(Sheet sheet, String sheetName, List<InitializationImportError> errors) {
        int headerIndex = findHeaderRow(sheet, sheetName);
        if (headerIndex < 0) {
            errors.add(new InitializationImportError(sheetName, 1, "headers", "", "Header row is missing"));
            return List.of();
        }
        Row headerRow = sheet.getRow(headerIndex);
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        List<String> headers = new ArrayList<>();
        boolean studentNameUsed = false;
        for (int index = 0; index < headerRow.getLastCellNum(); index++) {
            String raw = formatter.formatCellValue(headerRow.getCell(index)).trim();
            String canonical = canonicalHeader(raw, sheetName);
            if ("PROJECT_ASSIGNMENTS".equals(sheetName) && "name".equalsIgnoreCase(raw)) {
                canonical = studentNameUsed ? "supervisorName" : "studentName";
                studentNameUsed = true;
            }
            headers.add(canonical);
        }

        List<RowData> rows = new ArrayList<>();
        Map<String, String> previous = new HashMap<>();
        String currentProject = "";
        for (int rowIndex = headerIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            Map<String, String> data = new LinkedHashMap<>();
            boolean nonEmpty = false;
            for (int column = 0; column < headers.size(); column++) {
                String header = headers.get(column);
                if (header.isBlank()) continue;
                String value = formatter.formatCellValue(row.getCell(column)).trim();
                data.put(header, value);
                if (!value.isBlank()) nonEmpty = true;
            }
            if (!nonEmpty) continue;
            if ("PROJECT_ASSIGNMENTS".equals(sheetName)) {
                String explicitProject = data.getOrDefault("projectNumber", "");
                if (!explicitProject.isBlank() && !explicitProject.equalsIgnoreCase(currentProject)) {
                    previous.clear();
                    currentProject = explicitProject;
                }
                for (String field : PROJECT_METADATA) {
                    if (data.getOrDefault(field, "").isBlank() && previous.containsKey(field)) {
                        data.put(field, previous.get(field));
                    }
                    if (!data.getOrDefault(field, "").isBlank()) previous.put(field, data.get(field));
                }
            }
            rows.add(new RowData(sheetName, rowIndex + 1, data));
        }
        return rows;
    }

    private int findHeaderRow(Sheet sheet, String sheetName) {
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        for (int index = sheet.getFirstRowNum(); index <= Math.min(sheet.getLastRowNum(), 10); index++) {
            Row row = sheet.getRow(index);
            if (row == null) continue;
            Set<String> values = new HashSet<>();
            for (int column = 0; column < row.getLastCellNum(); column++) {
                values.add(canonicalHeader(formatter.formatCellValue(row.getCell(column)), sheetName));
            }
            if ("PROJECT_ASSIGNMENTS".equals(sheetName)
                    && values.contains("projectNumber") && values.contains("studentId")) return index;
            if ("STUDENTS".equals(sheetName) && values.contains("studentId") && values.contains("email")) return index;
            if (ACTOR_ROLES.containsKey(sheetName) && values.contains("actorId") && values.contains("email")) return index;
        }
        return -1;
    }

    private String canonicalHeader(String value, String sheetName) {
        String normalized = value == null ? "" : value.trim()
                .replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "studentid", "studentnumber", "stdid" -> "studentId";
            case "studentname" -> "studentName";
            case "actorid", "universityid", "professorid" -> "actorId";
            case "actorname", "fullname" -> "STUDENTS".equals(sheetName) ? "studentName" : "actorName";
            case "name" -> "STUDENTS".equals(sheetName) ? "studentName"
                    : ACTOR_ROLES.containsKey(sheetName) ? "actorName" : "name";
            case "email" -> "email";
            case "cohort" -> "cohort";
            case "trackcode", "track" -> "trackCode";
            case "level" -> "level";
            case "department" -> "department";
            case "specialization" -> "specialization";
            case "organization", "externalorganization" -> "organization";
            case "phone" -> "phone";
            case "accessexpiresat", "accessuntil", "expirydate", "expirationdate" -> "accessExpiresAt";
            case "status" -> "status";
            case "projectnumber", "project", "projectno" -> "projectNumber";
            case "projecttitle", "title" -> "projectTitle";
            case "projectabstract", "abstract" -> "projectAbstract";
            case "section" -> "section";
            case "supervisorid" -> "supervisorId";
            case "supervisorname" -> "supervisorName";
            case "reportphaseievaluatoremails" -> "reportPhaseIEvaluatorEmails";
            case "oralphaseievaluatoremails" -> "oralPhaseIEvaluatorEmails";
            case "reportphaseiievaluatoremails" -> "reportPhaseIIEvaluatorEmails";
            case "oralphaseiievaluatoremails" -> "oralPhaseIIEvaluatorEmails";
            case "industryguestemails" -> "industryGuestEmails";
            default -> value == null ? "" : value.trim();
        };
    }

    private Map<String, RowData> uniqueIndex(
            List<RowData> rows, String field, List<InitializationImportError> errors
    ) {
        Map<String, RowData> result = new LinkedHashMap<>();
        for (RowData row : rows) {
            String key = lower(row.value(field));
            if (!key.isBlank()) duplicate(row, field, key, result, "Duplicate value in workbook", errors);
        }
        return result;
    }

    private void duplicate(
            RowData row, String field, String key, Map<String, RowData> index,
            String message, List<InitializationImportError> errors
    ) {
        RowData previous = index.putIfAbsent(key, row);
        if (previous != null) {
            error(row, field, message + " (first: " + previous.sheet() + " row " + previous.rowNumber() + ")", errors);
        }
    }

    private void required(RowData row, String field, List<InitializationImportError> errors) {        if (row.value(field).isBlank()) error(row, field, "Required value is missing", errors);
    }

    private void error(RowData row, String field, String message, List<InitializationImportError> errors) {
        errors.add(new InitializationImportError(row.sheet(), row.rowNumber(), field, row.value(field), message));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_IMPORT_FILE", "Select a non-empty Excel workbook");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException("IMPORT_FILE_TOO_LARGE", "The workbook exceeds the 15 MB limit");
        }
        if (!safeFilename(file).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("INVALID_IMPORT_FORMAT", "The initialization workbook must use .xlsx");
        }
    }

    private List<String> splitEmails(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("[;,]")).map(String::trim).map(this::lower)
                .filter(email -> !email.isBlank()).distinct().toList();
    }

    private boolean isEvaluator(UserRole role) {
        return role == UserRole.SUPERVISOR || role == UserRole.FACULTY_EVALUATOR
                || role == UserRole.INDUSTRY_REPRESENTATIVE;
    }

    private LocalDateTime parseAccessExpiresAt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            try {
                return LocalDate.parse(value.trim()).atTime(23, 59, 59);
            } catch (DateTimeParseException nested) {
                throw new IllegalArgumentException("Invalid access expiration", nested);
            }
        }
    }

    private boolean different(Object left, Object right) {
        return !java.util.Objects.equals(left, right);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safeFilename(MultipartFile file) {
        return file.getOriginalFilename() == null ? "workbook.xlsx"
                : file.getOriginalFilename().replaceAll("[\r\n]", "");
    }

    private record Parsed(Map<String, List<RowData>> sheets, List<InitializationImportError> errors) {
        List<RowData> rows(String name) {
            return sheets.getOrDefault(name, List.of());
        }
    }

    private record RowData(String sheet, int rowNumber, Map<String, String> values) {
        String value(String name) {
            return values.getOrDefault(name, "").trim();
        }
    }

    private record RowLocation(String sheet, int rowNumber) {
    }

    private static class Counter {
        private int created;
        private int updated;
        private int unchanged;

        void record(boolean wasCreated, boolean wasChanged) {
            if (wasCreated) created++;
            else if (wasChanged) updated++;
            else unchanged++;
        }
    }
}
