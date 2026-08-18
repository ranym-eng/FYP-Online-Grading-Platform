package fyp_grading_platform.project;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private static final int MAX_STUDENTS = 5;

    private final TeamRepository teams;
    private final ProjectRepository projects;
    private final StudentProfileRepository students;
    private final CurrentUserService currentUsers;
    private final ProjectAccessService projectAccess;

    public TeamController(
            TeamRepository teams,
            ProjectRepository projects,
            StudentProfileRepository students,
            CurrentUserService currentUsers,
            ProjectAccessService projectAccess
    ) {
        this.teams = teams;
        this.projects = projects;
        this.students = students;
        this.currentUsers = currentUsers;
        this.projectAccess = projectAccess;
    }

    @PostMapping
    ApiResponse<Team> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody TeamRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        if (teams.findByProjectId(request.projectId()).isPresent()) {
            throw new BusinessException("PROJECT_TEAM_EXISTS", "This project already has a team");
        }
        Team team = new Team();
        apply(team, request);
        return ApiResponse.ok("Team created", teams.save(team));
    }

    @GetMapping
    ApiResponse<?> all(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User actor = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Teams", teams.findAll().stream()
                .filter(team -> team.getProject() != null && projectAccess.canView(actor, team.getProject().getId()))
                .toList());
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        Team team = findTeam(id);
        projectAccess.assertCanView(currentUsers.requireUser(authorization), team.getProject().getId());
        return ApiResponse.ok("Team", team);
    }

    @PutMapping("/{id}")
    ApiResponse<Team> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody TeamRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        Team team = findTeam(id);
        teams.findByProjectId(request.projectId()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("PROJECT_TEAM_EXISTS", "This project already has a team");
            }
        });
        apply(team, request);
        return ApiResponse.ok("Team updated", teams.save(team));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        Team team = findTeam(id);
        if (!team.getStudents().isEmpty()) {
            throw new BusinessException("TEAM_NOT_EMPTY", "Remove team students before deleting the team");
        }
        teams.delete(team);
        return ApiResponse.ok("Team deleted", null);
    }

    @GetMapping("/by-project/{projectId}")
    ApiResponse<?> byProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        projectAccess.assertCanView(currentUsers.requireUser(authorization), projectId);
        return ApiResponse.ok("Team", teams.findByProjectId(projectId));
    }

    @GetMapping("/by-academic-year/{year}")
    ApiResponse<?> byYear(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String year
    ) {
        User actor = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Teams", teams.findByAcademicYear(year).stream()
                .filter(team -> team.getProject() != null && projectAccess.canView(actor, team.getProject().getId()))
                .toList());
    }

    @PostMapping("/{teamId}/students/{studentId}")
    ApiResponse<Team> addStudent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID teamId,
            @PathVariable UUID studentId
    ) {
        currentUsers.requireAdmin(authorization);
        Team team = findTeam(teamId);
        if (team.getStudents().stream().anyMatch(student -> student.getId().equals(studentId))) {
            throw new BusinessException("STUDENT_ALREADY_IN_TEAM", "Student is already a member of this team");
        }
        if (team.getStudents().size() >= MAX_STUDENTS) {
            throw new BusinessException("TEAM_LIMIT_EXCEEDED", "A project can contain at most five students");
        }
        StudentProfile student = findStudent(studentId);
        assertStudentAvailable(team, student);
        team.getStudents().add(student);
        return ApiResponse.ok("Student added", teams.save(team));
    }

    @DeleteMapping("/{teamId}/students/{studentId}")
    ApiResponse<Team> removeStudent(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID teamId,
            @PathVariable UUID studentId
    ) {
        currentUsers.requireAdmin(authorization);
        Team team = findTeam(teamId);
        boolean removed = team.getStudents().removeIf(student -> student.getId().equals(studentId));
        if (!removed) throw new BusinessException("STUDENT_NOT_IN_TEAM", "Student is not a member of this team");
        return ApiResponse.ok("Student removed", teams.save(team));
    }

    @GetMapping("/{teamId}/students")
    ApiResponse<?> teamStudents(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID teamId
    ) {
        Team team = findTeam(teamId);
        projectAccess.assertCanView(currentUsers.requireUser(authorization), team.getProject().getId());
        return ApiResponse.ok("Team students", team.getStudents());
    }

    private Team findTeam(UUID id) {
        return teams.findById(id)
                .orElseThrow(() -> new BusinessException("TEAM_NOT_FOUND", "Team not found"));
    }

    private StudentProfile findStudent(UUID id) {
        return students.findById(id)
                .orElseThrow(() -> new BusinessException("STUDENT_NOT_FOUND", "Student not found"));
    }

    private void apply(Team team, TeamRequest request) {
        List<UUID> studentIds = request.studentIds() == null ? List.of() : request.studentIds();
        if (studentIds.size() > MAX_STUDENTS) {
            throw new BusinessException("TEAM_LIMIT_EXCEEDED", "A project can contain at most five students");
        }
        if (new HashSet<>(studentIds).size() != studentIds.size()) {
            throw new BusinessException("DUPLICATE_TEAM_MEMBER", "A student can appear only once in a team");
        }

        team.setName(request.name().trim());
        team.setSection(request.section());
        team.setAcademicYear(request.academicYear().trim());
        team.setProject(projects.findById(request.projectId())
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found")));
        team.getStudents().clear();
        studentIds.forEach(id -> {
            StudentProfile student = findStudent(id);
            assertStudentAvailable(team, student);
            team.getStudents().add(student);
        });
    }

    private void assertStudentAvailable(Team target, StudentProfile student) {
        String cohort = student.getCohort() == null || student.getCohort().isBlank()
                ? student.getAcademicYear()
                : student.getCohort();
        if (cohort != null && !cohort.isBlank() && !cohort.equalsIgnoreCase(target.getAcademicYear())) {
            throw new BusinessException("STUDENT_COHORT_MISMATCH", "Student cohort does not match the project cohort");
        }
        boolean usedElsewhere = teams.findAll().stream()
                .filter(existing -> target.getId() == null || !existing.getId().equals(target.getId()))
                .filter(existing -> existing.getAcademicYear().equalsIgnoreCase(target.getAcademicYear()))
                .anyMatch(existing -> existing.getStudents().stream()
                        .anyMatch(member -> member.getId().equals(student.getId())));
        if (usedElsewhere) {
            throw new BusinessException("STUDENT_ALREADY_ASSIGNED", "Student is already assigned to another project in this cohort");
        }
    }
}