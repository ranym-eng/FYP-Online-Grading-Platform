package fyp_grading_platform.importing;

import fyp_grading_platform.audit.AuditService;
import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.project.Phase;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectEvaluatorAssignment;
import fyp_grading_platform.project.ProjectEvaluatorAssignmentRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.ProjectSupervisorAssignment;
import fyp_grading_platform.project.ProjectSupervisorAssignmentRepository;
import fyp_grading_platform.project.Team;
import fyp_grading_platform.project.TeamRepository;
import fyp_grading_platform.project.Track;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.user.EvaluatorProfile;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.StudentController;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class PlatformInitializationImportService {
    private static final long MAX_FILE_BYTES = 15L * 1024 * 1024;
    private static final List<String> SHEET_ORDER = List.of(
            "TRACKS", "STUDENTS", "ACTORS", "PROJECTS", "TEAM_MEMBERS",
            "SUPERVISORS", "EVALUATOR_ASSIGNMENTS", "PHASES"
    );
    private static final Map<String, Set<String>> REQUIRED_HEADERS = Map.of(
            "TRACKS", Set.of("code", "name", "description", "active"),
            "STUDENTS", Set.of("studentNumber", "fullName", "email", "cohort", "academicYear", "trackCode", "level"),
            "ACTORS", Set.of("universityId", "fullName", "email", "role", "department", "specialization", "externalOrganization", "phone", "temporaryPassword", "status"),
            "PROJECTS", Set.of("projectNumber", "title", "academicYear", "trackCode", "status", "abstractText", "teamName", "section"),
            "TEAM_MEMBERS", Set.of("projectNumber", "studentNumber"),
            "SUPERVISORS", Set.of("projectNumber", "supervisorEmail"),
            "EVALUATOR_ASSIGNMENTS", Set.of("projectNumber", "evaluatorEmail", "evaluationType"),
            "PHASES", Set.of("name", "type", "academicYear", "startDate", "deadline", "status")
    );
    private static final Set<UserRole> EVALUATOR_ROLES = Set.of(
            UserRole.SUPERVISOR,
            UserRole.FACULTY_EVALUATOR,
            UserRole.INDUSTRY_REPRESENTATIVE
    );
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TrackRepository tracks;
    private final StudentProfileRepository students;
    private final UserRepository users;
    private final EvaluatorProfileRepository evaluatorProfiles;
    private final ProjectRepository projects;
    private final TeamRepository teams;
    private final ProjectSupervisorAssignmentRepository supervisors;
    private final ProjectEvaluatorAssignmentRepository evaluatorAssignments;
    private final PhaseRepository phases;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public PlatformInitializationImportService(
            TrackRepository tracks,
            StudentProfileRepository students,
            UserRepository users,
            EvaluatorProfileRepository evaluatorProfiles,
            ProjectRepository projects,
            TeamRepository teams,
            ProjectSupervisorAssignmentRepository supervisors,
            ProjectEvaluatorAssignmentRepository evaluatorAssignments,
            PhaseRepository phases,
            PasswordEncoder passwordEncoder,
            AuditService audit
    ) {
        this.tracks = tracks;
        this.students = students;
        this.users = users;
        this.evaluatorProfiles = evaluatorProfiles;
        this.projects = projects;
        this.teams = teams;
        this.supervisors = supervisors;
        this.evaluatorAssignments = evaluatorAssignments;
        this.phases = phases;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public InitializationImportReport preview(MultipartFile file) {
        ParsedWorkbook parsed = parse(file);
        return validate(parsed, true);
    }

    @Transactional
    public InitializationImportReport importWorkbook(MultipartFile file, User actor) {
        ParsedWorkbook parsed = parse(file);
        InitializationImportReport validation = validate(parsed, false);
        if (!validation.importable()) return validation;

        Map<String, Counters> counters = SHEET_ORDER.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> new Counters(), (left, right) -> left, LinkedHashMap::new));

        importTracks(parsed.rows("TRACKS"), counters.get("TRACKS"));
        importStudents(parsed.rows("STUDENTS"), counters.get("STUDENTS"));
        importActors(parsed.rows("ACTORS"), counters.get("ACTORS"));
        importProjects(parsed.rows("PROJECTS"), counters.get("PROJECTS"));
        importTeamMembers(parsed.rows("TEAM_MEMBERS"), counters.get("TEAM_MEMBERS"));
        importSupervisors(parsed.rows("SUPERVISORS"), counters.get("SUPERVISORS"));
        importEvaluatorAssignments(parsed.rows("EVALUATOR_ASSIGNMENTS"), counters.get("EVALUATOR_ASSIGNMENTS"));
        importPhases(parsed.rows("PHASES"), counters.get("PHASES"));

        List<InitializationSheetSummary> summaries = SHEET_ORDER.stream()
                .map(sheet -> {
                    Counters count = counters.get(sheet);
                    int rows = parsed.rows(sheet).size();
                    return new InitializationSheetSummary(sheet, rows, rows, count.created, count.updated, count.unchanged);
                })
                .toList();
        int totalRows = summaries.stream().mapToInt(InitializationSheetSummary::totalRows).sum();

        audit.record(
                actor.getId(),
                "PLATFORM_INITIALIZED_FROM_WORKBOOK",
                "PlatformData",
                actor.getId(),
                null,
                "file=" + safeFilename(file) + ", rows=" + totalRows
        );
        return new InitializationImportReport(false, true, totalRows, totalRows, summaries, List.of());
    }

    private InitializationImportReport validate(ParsedWorkbook workbook, boolean preview) {
        List<InitializationImportError> errors = new ArrayList<>(workbook.errors());
        Map<String, Set<String>> fileKeys = new HashMap<>();
        fileKeys.put("track", keys(workbook.rows("TRACKS"), "code", errors));
        fileKeys.put("student", keys(workbook.rows("STUDENTS"), "studentNumber", errors));
        fileKeys.put("studentEmail", keys(workbook.rows("STUDENTS"), "email", errors));
        fileKeys.put("actorEmail", keys(workbook.rows("ACTORS"), "email", errors));
        fileKeys.put("universityId", keys(workbook.rows("ACTORS"), "universityId", errors));
        fileKeys.put("project", keys(workbook.rows("PROJECTS"), "projectNumber", errors));
        duplicateCompoundKeys(workbook.rows("TEAM_MEMBERS"), List.of("projectNumber", "studentNumber"), errors);
        duplicateCompoundKeys(workbook.rows("SUPERVISORS"), List.of("projectNumber", "supervisorEmail"), errors);
        duplicateCompoundKeys(workbook.rows("EVALUATOR_ASSIGNMENTS"), List.of("projectNumber", "evaluatorEmail", "evaluationType"), errors);
        duplicateCompoundKeys(workbook.rows("PHASES"), List.of("academicYear", "type"), errors);

        Set<String> knownTracks = tracks.findAll().stream().map(track -> upper(track.getCode())).collect(Collectors.toSet());
        knownTracks.addAll(fileKeys.get("track"));
        List<StudentProfile> existingStudentRecords = students.findAll();
        Set<String> knownStudents = existingStudentRecords.stream().map(StudentProfile::getStudentNumber).filter(value -> value != null).collect(Collectors.toSet());
        knownStudents.addAll(fileKeys.get("student"));
        Map<String, String> studentEmailOwners = existingStudentRecords.stream()
                .filter(student -> student.getEmail() != null && student.getStudentNumber() != null)
                .collect(Collectors.toMap(student -> lower(student.getEmail()), StudentProfile::getStudentNumber, (left, right) -> left));
        Set<String> knownProjects = projects.findAll().stream().map(Project::getProjectNumber).filter(value -> value != null).map(this::upper).collect(Collectors.toSet());
        knownProjects.addAll(fileKeys.get("project"));
        Map<String, UserRole> knownActors = users.findAll().stream()
                .filter(user -> user.getEmail() != null)
                .collect(Collectors.toMap(user -> lower(user.getEmail()), User::getRole, (left, right) -> left));

        validateTracks(workbook.rows("TRACKS"), errors);
        validateStudents(workbook.rows("STUDENTS"), knownTracks, studentEmailOwners, errors);
        validateActors(workbook.rows("ACTORS"), knownActors, errors);
        workbook.rows("ACTORS").forEach(row -> enumValue(row, "role", UserRole.class).ifPresent(role -> knownActors.put(lower(row.value("email")), role)));
        validateProjects(workbook.rows("PROJECTS"), knownTracks, errors);
        validateTeamMembers(workbook.rows("TEAM_MEMBERS"), knownProjects, knownStudents, errors);
        validateSupervisors(workbook.rows("SUPERVISORS"), knownProjects, knownActors, errors);
        validateEvaluatorAssignments(workbook.rows("EVALUATOR_ASSIGNMENTS"), knownProjects, knownActors, errors);
        validatePhases(workbook.rows("PHASES"), errors);

        Set<RowLocation> invalidRows = errors.stream()
                .filter(error -> error.rowNumber() > 0)
                .map(error -> new RowLocation(error.sheet(), error.rowNumber()))
                .collect(Collectors.toSet());
        List<InitializationSheetSummary> summaries = SHEET_ORDER.stream()
                .map(sheet -> {
                    int total = workbook.rows(sheet).size();
                    int invalid = (int) invalidRows.stream().filter(location -> location.sheet().equals(sheet)).count();
                    return new InitializationSheetSummary(sheet, total, Math.max(0, total - invalid), 0, 0, 0);
                })
                .toList();
        int totalRows = summaries.stream().mapToInt(InitializationSheetSummary::totalRows).sum();
        int validRows = summaries.stream().mapToInt(InitializationSheetSummary::validRows).sum();
        return new InitializationImportReport(preview, errors.isEmpty(), totalRows, validRows, summaries, List.copyOf(errors));
    }

    private void validateTracks(List<ImportRow> rows, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            required(row, "code", errors);
            required(row, "name", errors);
            booleanValue(row, "active", errors);
        }
    }

    private void validateStudents(List<ImportRow> rows, Set<String> knownTracks, Map<String, String> emailOwners, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            String number = StudentController.normalizeStudentNumber(row.value("studentNumber"));
            String email = lower(row.value("email"));
            required(row, "studentNumber", errors);
            required(row, "fullName", errors);
            required(row, "email", errors);
            required(row, "cohort", errors);
            required(row, "academicYear", errors);
            required(row, "trackCode", errors);
            required(row, "level", errors);
            if (!row.value("cohort").isBlank() && !normalizeCohort(row.value("cohort")).matches("(?:19|20)\\d{2}")) {
                error(row, "cohort", "Cohort must use YY or YYYY format", errors);
            }
            if (!number.isBlank() && !number.matches("\\d{5,12}")) error(row, "studentNumber", "Student ID must contain 5 to 12 digits", errors);
            if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) error(row, "email", "Invalid email address", errors);
            if (!number.isBlank() && !email.isBlank() && !email.equals("s" + number + "@student.squ.edu.om")) {
                error(row, "email", "Expected SQU student email: s" + number + "@student.squ.edu.om", errors);
            }
            String emailOwner = emailOwners.get(email);
            if (emailOwner != null && !emailOwner.equals(number)) {
                error(row, "email", "Email already belongs to another student", errors);
            }
            reference(row, "trackCode", upper(row.value("trackCode")), knownTracks, "Unknown track code", errors);
        }
    }

    private void validateActors(List<ImportRow> rows, Map<String, UserRole> existing, List<InitializationImportError> errors) {
        Set<String> existingUniversityIds = users.findAll().stream()
                .filter(user -> user.getUniversityId() != null)
                .map(user -> user.getUniversityId().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (ImportRow row : rows) {
            required(row, "universityId", errors);
            required(row, "fullName", errors);
            required(row, "email", errors);
            required(row, "role", errors);
            enumValue(row, "role", UserRole.class, errors).ifPresent(role -> {
                if (role == UserRole.INDUSTRY_REPRESENTATIVE && row.value("externalOrganization").isBlank()) {
                    error(row, "externalOrganization", "Industry representatives require an organization", errors);
                }
            });
            enumValue(row, "status", UserStatus.class, errors);
            String email = lower(row.value("email"));
            if (!email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                error(row, "email", "Invalid email address", errors);
            }
            if (!existing.containsKey(email) && row.value("temporaryPassword").isBlank()) {
                error(row, "temporaryPassword", "A temporary password is required for a new actor", errors);
            }
            if (existingUniversityIds.contains(lower(row.value("universityId"))) && !existing.containsKey(email)) {
                error(row, "universityId", "University ID already belongs to another account", errors);
            }
        }
    }

    private void validateProjects(List<ImportRow> rows, Set<String> knownTracks, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            required(row, "projectNumber", errors);
            required(row, "title", errors);
            required(row, "academicYear", errors);
            required(row, "trackCode", errors);
            required(row, "teamName", errors);
            reference(row, "trackCode", upper(row.value("trackCode")), knownTracks, "Unknown track code", errors);
        }
    }
    private void validateTeamMembers(List<ImportRow> rows, Set<String> knownProjects, Set<String> knownStudents, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            required(row, "projectNumber", errors);
            required(row, "studentNumber", errors);
            reference(row, "projectNumber", upper(row.value("projectNumber")), knownProjects, "Unknown project", errors);
            reference(row, "studentNumber", StudentController.normalizeStudentNumber(row.value("studentNumber")), knownStudents, "Unknown student", errors);
        }
    }

    private void validateSupervisors(List<ImportRow> rows, Set<String> knownProjects, Map<String, UserRole> knownActors, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            required(row, "projectNumber", errors);
            required(row, "supervisorEmail", errors);
            reference(row, "projectNumber", upper(row.value("projectNumber")), knownProjects, "Unknown project", errors);
            UserRole role = knownActors.get(lower(row.value("supervisorEmail")));
            if (role == null) error(row, "supervisorEmail", "Unknown actor email", errors);
            else if (role != UserRole.SUPERVISOR) error(row, "supervisorEmail", "Actor role must be SUPERVISOR", errors);
        }
    }

    private void validateEvaluatorAssignments(List<ImportRow> rows, Set<String> knownProjects, Map<String, UserRole> knownActors, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            required(row, "projectNumber", errors);
            required(row, "evaluatorEmail", errors);
            required(row, "evaluationType", errors);
            reference(row, "projectNumber", upper(row.value("projectNumber")), knownProjects, "Unknown project", errors);
            UserRole role = knownActors.get(lower(row.value("evaluatorEmail")));
            if (role == null) error(row, "evaluatorEmail", "Unknown actor email", errors);
            else if (!EVALUATOR_ROLES.contains(role)) error(row, "evaluatorEmail", "Actor is not an evaluator", errors);
            enumValue(row, "evaluationType", EvaluationType.class, errors).ifPresent(type -> {
                if (role == UserRole.INDUSTRY_REPRESENTATIVE && type != EvaluationType.DEMO_DAY_INDUSTRY) {
                    error(row, "evaluationType", "Industry representatives can evaluate only DEMO_DAY_INDUSTRY", errors);
                }
                if (role == UserRole.SUPERVISOR && type != EvaluationType.SUPERVISOR_PHASE_I && type != EvaluationType.SUPERVISOR_PHASE_II) {
                    error(row, "evaluationType", "Supervisors can use only supervisor forms", errors);
                }
                if (role == UserRole.FACULTY_EVALUATOR && (type == EvaluationType.DEMO_DAY_INDUSTRY || type.name().startsWith("SUPERVISOR_"))) {
                    error(row, "evaluationType", "Faculty evaluators use only report and oral forms", errors);
                }
            });
        }
    }

    private void validatePhases(List<ImportRow> rows, List<InitializationImportError> errors) {
        for (ImportRow row : rows) {
            required(row, "name", errors);
            required(row, "type", errors);
            required(row, "academicYear", errors);
            required(row, "startDate", errors);
            required(row, "deadline", errors);
            enumValue(row, "type", PhaseType.class, errors);
            enumValue(row, "status", PhaseStatus.class, errors);
            LocalDateTime start = dateTime(row, "startDate", errors);
            LocalDateTime deadline = dateTime(row, "deadline", errors);
            if (start != null && deadline != null && !deadline.isAfter(start)) {
                error(row, "deadline", "Deadline must be after the start date", errors);
            }
        }
    }

    private void importTracks(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            String code = upper(row.value("code"));
            Track track = tracks.findByCode(code).orElse(null);
            boolean created = track == null;
            if (created) track = new Track();
            boolean changed = created || changed(track.getCode(), code) || changed(track.getName(), row.value("name"))
                    || changed(track.getDescription(), row.value("description")) || track.isActive() != parseBoolean(row.value("active"));
            track.setCode(code);
            track.setName(row.value("name"));
            track.setDescription(row.value("description"));
            track.setActive(parseBoolean(row.value("active")));
            if (changed) tracks.save(track);
            counts.record(created, changed);
        }
    }

    private void importStudents(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            String number = StudentController.normalizeStudentNumber(row.value("studentNumber"));
            StudentProfile student = students.findByStudentNumber(number).orElse(null);
            boolean created = student == null;
            if (created) student = new StudentProfile();
            String cohort = normalizeCohort(row.value("cohort"));
            boolean changed = created || changed(student.getFullName(), row.value("fullName"))
                    || changed(student.getEmail(), lower(row.value("email"))) || changed(student.getCohort(), cohort)
                    || changed(student.getAcademicYear(), row.value("academicYear")) || changed(student.getTrackCode(), upper(row.value("trackCode")))
                    || changed(student.getLevel(), row.value("level"));
            student.setStudentNumber(number);
            student.setFullName(row.value("fullName"));
            student.setEmail(lower(row.value("email")));
            student.setCohort(cohort);
            student.setAcademicYear(row.value("academicYear"));
            student.setTrackCode(upper(row.value("trackCode")));
            student.setLevel(row.value("level"));
            if (changed) students.save(student);
            counts.record(created, changed);
        }
    }

    private void importActors(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            String email = lower(row.value("email"));
            User user = users.findByEmailIgnoreCase(email).orElse(null);
            boolean created = user == null;
            if (created) user = new User();
            UserRole role = enumRequired(row.value("role"), UserRole.class);
            UserStatus status = enumOrDefault(row.value("status"), UserStatus.class, UserStatus.ACTIVE);
            boolean changed = created || changed(user.getUniversityId(), row.value("universityId"))
                    || changed(user.getFullName(), row.value("fullName")) || changed(user.getEmail(), email)
                    || changed(user.getPhone(), row.value("phone")) || user.getRole() != role || user.getStatus() != status;
            user.setUniversityId(row.value("universityId"));
            user.setFullName(row.value("fullName"));
            user.setEmail(email);
            user.setPhone(row.value("phone"));
            user.setRole(role);
            user.setStatus(status);
            if (created) user.setPasswordHash(passwordEncoder.encode(row.value("temporaryPassword")));
            user = users.save(user);
            if (EVALUATOR_ROLES.contains(role)) {
                EvaluatorProfile profile = evaluatorProfiles.findByUserId(user.getId()).orElse(null);
                boolean profileCreated = profile == null;
                if (profileCreated) profile = new EvaluatorProfile();
                profile.setUser(user);
                profile.setDepartment(row.value("department"));
                profile.setSpecialization(row.value("specialization"));
                profile.setExternalOrganization(row.value("externalOrganization"));
                profile.setExternal(role == UserRole.INDUSTRY_REPRESENTATIVE);
                evaluatorProfiles.save(profile);
                changed = changed || profileCreated;
            }
            counts.record(created, changed);
        }
    }

    private void importProjects(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            String number = upper(row.value("projectNumber"));
            Project project = projects.findByProjectNumberIgnoreCase(number).orElse(null);
            boolean created = project == null;
            if (created) project = new Project();
            Track track = tracks.findByCode(upper(row.value("trackCode"))).orElseThrow();
            String status = row.value("status").isBlank() ? "ACTIVE" : upper(row.value("status"));
            boolean changed = created || changed(project.getTitle(), row.value("title"))
                    || changed(project.getAcademicYear(), row.value("academicYear")) || changed(project.getStatus(), status)
                    || changed(project.getAbstractText(), row.value("abstractText"))
                    || project.getTrack() == null || !project.getTrack().getId().equals(track.getId());
            project.setProjectNumber(number);
            project.setTitle(row.value("title"));
            project.setAcademicYear(row.value("academicYear"));
            project.setStatus(status);
            project.setAbstractText(row.value("abstractText"));
            project.setTrack(track);
            project = projects.save(project);

            Team team = teams.findByProjectId(project.getId()).orElse(null);
            if (team == null) team = new Team();
            team.setProject(project);
            team.setName(row.value("teamName"));
            team.setSection(row.value("section"));
            team.setAcademicYear(row.value("academicYear"));
            teams.save(team);
            counts.record(created, changed);
        }
    }

    private void importTeamMembers(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            Project project = projects.findByProjectNumberIgnoreCase(row.value("projectNumber")).orElseThrow();
            Team team = teams.findByProjectId(project.getId()).orElseThrow();
            StudentProfile student = students.findByStudentNumber(StudentController.normalizeStudentNumber(row.value("studentNumber"))).orElseThrow();
            boolean changed = team.getStudents().add(student);
            if (changed) teams.save(team);
            counts.record(changed, changed);
        }
    }

    private void importSupervisors(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            Project project = projects.findByProjectNumberIgnoreCase(row.value("projectNumber")).orElseThrow();
            EvaluatorProfile supervisor = evaluatorByEmail(row.value("supervisorEmail"));
            ProjectSupervisorAssignment assignment = supervisors.findByProjectIdAndSupervisorId(project.getId(), supervisor.getId()).orElse(null);
            boolean created = assignment == null;
            if (created) assignment = new ProjectSupervisorAssignment();
            boolean changed = created || !assignment.isActive();
            assignment.setProject(project);
            assignment.setSupervisor(supervisor);
            assignment.setActive(true);
            if (changed) supervisors.save(assignment);
            ensureEvaluationAssignment(project, supervisor, EvaluationType.SUPERVISOR_PHASE_I);
            ensureEvaluationAssignment(project, supervisor, EvaluationType.SUPERVISOR_PHASE_II);
            counts.record(created, changed);
        }
    }

    private void importEvaluatorAssignments(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            Project project = projects.findByProjectNumberIgnoreCase(row.value("projectNumber")).orElseThrow();
            EvaluatorProfile evaluator = evaluatorByEmail(row.value("evaluatorEmail"));
            EvaluationType type = enumRequired(row.value("evaluationType"), EvaluationType.class);
            boolean created = ensureEvaluationAssignment(project, evaluator, type);
            counts.record(created, created);
        }
    }

    private void importPhases(List<ImportRow> rows, Counters counts) {
        for (ImportRow row : rows) {
            PhaseType type = enumRequired(row.value("type"), PhaseType.class);
            Phase phase = phases.findByAcademicYearAndType(row.value("academicYear"), type).orElse(null);
            boolean created = phase == null;
            if (created) phase = new Phase();
            LocalDateTime start = parseDateTime(row.value("startDate"));
            LocalDateTime deadline = parseDateTime(row.value("deadline"));
            PhaseStatus status = enumOrDefault(row.value("status"), PhaseStatus.class, PhaseStatus.NOT_STARTED);
            boolean changed = created || changed(phase.getName(), row.value("name")) || phase.getType() != type
                    || changed(phase.getAcademicYear(), row.value("academicYear")) || !start.equals(phase.getStartDate())
                    || !deadline.equals(phase.getDeadline()) || phase.getStatus() != status;
            phase.setName(row.value("name"));
            phase.setType(type);
            phase.setAcademicYear(row.value("academicYear"));
            phase.setStartDate(start);
            phase.setDeadline(deadline);
            phase.setStatus(status);
            if (changed) phases.save(phase);
            counts.record(created, changed);
        }
    }

    private boolean ensureEvaluationAssignment(Project project, EvaluatorProfile evaluator, EvaluationType type) {
        ProjectEvaluatorAssignment assignment = evaluatorAssignments
                .findByProjectIdAndEvaluatorIdAndEvaluationType(project.getId(), evaluator.getId(), type)
                .orElse(null);
        boolean created = assignment == null;
        boolean changed = created || !assignment.isActive();
        if (created) assignment = new ProjectEvaluatorAssignment();
        assignment.setProject(project);
        assignment.setEvaluator(evaluator);
        assignment.setEvaluationType(type);
        assignment.setActive(true);
        if (changed) evaluatorAssignments.save(assignment);
        return created;
    }

    private EvaluatorProfile evaluatorByEmail(String email) {
        User user = users.findByEmailIgnoreCase(email).orElseThrow();
        return evaluatorProfiles.findByUserId(user.getId()).orElseThrow();
    }

    private ParsedWorkbook parse(MultipartFile file) {
        validateFile(file);
        Map<String, List<ImportRow>> rows = new LinkedHashMap<>();
        List<InitializationImportError> errors = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            for (String sheetName : SHEET_ORDER) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    errors.add(new InitializationImportError(sheetName, 0, "sheet", "", "Required worksheet is missing"));
                    rows.put(sheetName, List.of());
                    continue;
                }
                rows.put(sheetName, readSheet(sheet, formatter, errors));
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessException("INVALID_INITIALIZATION_WORKBOOK", "The initialization workbook could not be read: " + exception.getMessage());
        }
        return new ParsedWorkbook(rows, errors);
    }

    private List<ImportRow> readSheet(Sheet sheet, DataFormatter formatter, List<InitializationImportError> errors) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            errors.add(new InitializationImportError(sheet.getSheetName(), 1, "headers", "", "Header row is missing"));
            return List.of();
        }
        Map<Integer, String> columns = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            String header = formatter.formatCellValue(cell).trim();
            if (!header.isBlank()) columns.put(cell.getColumnIndex(), header);
        }
        Set<String> required = REQUIRED_HEADERS.get(sheet.getSheetName());
        Set<String> missing = new LinkedHashSet<>(required);
        missing.removeAll(columns.values());
        missing.forEach(header -> errors.add(new InitializationImportError(
                sheet.getSheetName(), 1, header, "", "Required column is missing"
        )));
        List<ImportRow> result = new ArrayList<>();
        for (int index = 1; index <= sheet.getLastRowNum(); index++) {
            Row row = sheet.getRow(index);
            if (row == null) continue;
            Map<String, String> values = new LinkedHashMap<>();
            columns.forEach((column, header) -> values.put(header, cellValue(row.getCell(column), formatter)));
            if (values.values().stream().allMatch(String::isBlank)) continue;
            result.add(new ImportRow(sheet.getSheetName(), index + 1, values));
        }
        return result;
    }

    private String cellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().format(DISPLAY_DATE_TIME);
        }
        return formatter.formatCellValue(cell).trim().replaceFirst("\\.0$", "");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("EMPTY_IMPORT_FILE", "Select a non-empty .xlsx file");
        if (file.getSize() > MAX_FILE_BYTES) throw new BusinessException("IMPORT_FILE_TOO_LARGE", "The import workbook must not exceed 15 MB");
        if (!safeFilename(file).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("UNSUPPORTED_IMPORT_FORMAT", "The complete initialization import requires an .xlsx workbook");
        }
    }

    private Set<String> keys(List<ImportRow> rows, String field, List<InitializationImportError> errors) {
        Set<String> values = new HashSet<>();
        for (ImportRow row : rows) {
            String value = normalizeKey(field, row.value(field));
            if (value.isBlank()) continue;
            if (!values.add(value)) error(row, field, "Duplicate key in worksheet", errors);
        }
        return values;
    }

    private void duplicateCompoundKeys(List<ImportRow> rows, List<String> fields, List<InitializationImportError> errors) {
        Set<String> values = new HashSet<>();
        for (ImportRow row : rows) {
            String value = fields.stream().map(field -> normalizeKey(field, row.value(field))).collect(Collectors.joining("|"));
            if (!values.add(value)) error(row, String.join("+", fields), "Duplicate assignment in worksheet", errors);
        }
    }

    private String normalizeKey(String field, String value) {
        if (field.toLowerCase(Locale.ROOT).contains("email")) return lower(value);
        if (field.equals("studentNumber")) return StudentController.normalizeStudentNumber(value);
        return upper(value);
    }

    private void required(ImportRow row, String field, List<InitializationImportError> errors) {
        if (row.value(field).isBlank()) error(row, field, "Required value is missing", errors);
    }

    private void reference(ImportRow row, String field, String value, Set<String> known, String message, List<InitializationImportError> errors) {
        if (!value.isBlank() && !known.contains(value)) error(row, field, message, errors);
    }

    private void booleanValue(ImportRow row, String field, List<InitializationImportError> errors) {
        String value = lower(row.value(field));
        if (!Set.of("true", "false", "yes", "no", "1", "0").contains(value)) error(row, field, "Expected TRUE or FALSE", errors);
    }

    private <E extends Enum<E>> java.util.Optional<E> enumValue(ImportRow row, String field, Class<E> type) {
        if (row.value(field).isBlank()) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(Enum.valueOf(type, upper(row.value(field))));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private <E extends Enum<E>> java.util.Optional<E> enumValue(ImportRow row, String field, Class<E> type, List<InitializationImportError> errors) {
        java.util.Optional<E> value = enumValue(row, field, type);
        if (!row.value(field).isBlank() && value.isEmpty()) {
            error(row, field, "Expected one of: " + Arrays.toString(type.getEnumConstants()), errors);
        }
        return value;
    }

    private LocalDateTime dateTime(ImportRow row, String field, List<InitializationImportError> errors) {
        if (row.value(field).isBlank()) return null;
        try {
            return parseDateTime(row.value(field));
        } catch (DateTimeParseException exception) {
            error(row, field, "Expected date format yyyy-MM-dd HH:mm", errors);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() == 16) return LocalDateTime.parse(normalized, DISPLAY_DATE_TIME);
        return LocalDateTime.parse(value.trim());
    }

    private void error(ImportRow row, String field, String message, List<InitializationImportError> errors) {
        errors.add(new InitializationImportError(row.sheet(), row.rowNumber(), field, row.value(field), message));
    }

    private boolean changed(Object before, Object after) {
        return !java.util.Objects.equals(before == null ? "" : before, after == null ? "" : after);
    }

    private boolean parseBoolean(String value) {
        return Set.of("true", "yes", "1").contains(lower(value));
    }

    private String normalizeCohort(String value) {
        String normalized = value.trim().replaceFirst("\\.0$", "");
        return normalized.matches("\\d{2}") ? "20" + normalized : normalized;
    }

    private String lower(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String upper(String value) { return value == null ? "" : value.trim().toUpperCase(Locale.ROOT); }

    private <E extends Enum<E>> E enumRequired(String value, Class<E> type) {
        return Enum.valueOf(type, upper(value));
    }

    private <E extends Enum<E>> E enumOrDefault(String value, Class<E> type, E fallback) {
        return value == null || value.isBlank() ? fallback : enumRequired(value, type);
    }

    private String safeFilename(MultipartFile file) {
        String value = file.getOriginalFilename();
        return value == null || value.isBlank() ? "initialization.xlsx" : value.replaceAll("[\\r\\n]", "");
    }

    private record ImportRow(String sheet, int rowNumber, Map<String, String> values) {
        String value(String field) { return values.getOrDefault(field, "").trim(); }
    }
    private record ParsedWorkbook(Map<String, List<ImportRow>> sheets, List<InitializationImportError> errors) {
        List<ImportRow> rows(String sheet) { return sheets.getOrDefault(sheet, List.of()); }
    }
    private record RowLocation(String sheet, int rowNumber) {}
    private static final class Counters {
        private int created;
        private int updated;
        private int unchanged;
        private void record(boolean isNew, boolean changed) {
            if (isNew) created++;
            else if (changed) updated++;
            else unchanged++;
        }
    }
}
