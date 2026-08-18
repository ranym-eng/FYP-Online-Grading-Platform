package fyp_grading_platform.project;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import fyp_grading_platform.user.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
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
        apply(team, request);
        return ApiResponse.ok("Team updated", teams.save(team));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        teams.deleteById(id);
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
        StudentProfile student = students.findById(studentId)
                .orElseThrow(() -> new BusinessException("STUDENT_NOT_FOUND", "Student not found"));
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
        team.getStudents().removeIf(student -> student.getId().equals(studentId));
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

    private void apply(Team team, TeamRequest request) {
        team.setName(request.name());
        team.setSection(request.section());
        team.setAcademicYear(request.academicYear());
        team.setProject(projects.findById(request.projectId())
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found")));
        team.getStudents().clear();
        if (request.studentIds() != null) {
            request.studentIds().forEach(id -> team.getStudents().add(students.findById(id)
                    .orElseThrow(() -> new BusinessException("STUDENT_NOT_FOUND", "Student not found"))));
        }
    }
}