package fyp_grading_platform.project;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects")
public class ProjectController {
    private final ProjectRepository projects;
    private final TrackRepository tracks;
    private final ProjectSupervisorAssignmentRepository supervisors;
    private final ProjectEvaluatorAssignmentRepository evaluators;
    private final EvaluatorProfileRepository evaluatorProfiles;
    private final CurrentUserService currentUsers;
    private final ProjectAccessService projectAccess;

    public ProjectController(
            ProjectRepository projects,
            TrackRepository tracks,
            ProjectSupervisorAssignmentRepository supervisors,
            ProjectEvaluatorAssignmentRepository evaluators,
            EvaluatorProfileRepository evaluatorProfiles,
            CurrentUserService currentUsers,
            ProjectAccessService projectAccess
    ) {
        this.projects = projects;
        this.tracks = tracks;
        this.supervisors = supervisors;
        this.evaluators = evaluators;
        this.evaluatorProfiles = evaluatorProfiles;
        this.currentUsers = currentUsers;
        this.projectAccess = projectAccess;
    }

    @PostMapping
    @Operation(summary = "Create an FYP project")
    ApiResponse<Project> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ProjectRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        assertProjectNumberAvailable(request.projectNumber(), null);
        Project project = new Project();
        apply(project, request);
        return ApiResponse.ok("Project created", projects.save(project));
    }

    @GetMapping
    ApiResponse<?> all(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok("Projects", projectAccess.visibleProjects(currentUsers.requireUser(authorization)));
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        projectAccess.assertCanView(currentUsers.requireUser(authorization), id);
        return ApiResponse.ok("Project", projects.findById(id)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found")));
    }

    @PutMapping("/{id}")
    ApiResponse<Project> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        Project project = projects.findById(id)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        assertProjectNumberAvailable(request.projectNumber(), id);
        apply(project, request);
        return ApiResponse.ok("Project updated", projects.save(project));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        if (!projects.existsById(id)) {
            throw new BusinessException("PROJECT_NOT_FOUND", "Project not found");
        }
        projects.deleteById(id);
        return ApiResponse.ok("Project deleted", null);
    }

    @PatchMapping("/{id}/status")
    ApiResponse<Project> status(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        currentUsers.requireAdmin(authorization);
        Project project = projects.findById(id)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        project.setStatus(status);
        return ApiResponse.ok("Project status updated", projects.save(project));
    }

    @GetMapping("/by-track/{trackId}")
    ApiResponse<?> byTrack(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID trackId
    ) {
        return ApiResponse.ok("Projects", projectAccess.visibleProjects(currentUsers.requireUser(authorization)).stream()
                .filter(project -> project.getTrack() != null && project.getTrack().getId().equals(trackId))
                .toList());
    }

    @GetMapping("/by-academic-year/{year}")
    ApiResponse<?> byYear(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String year
    ) {
        return ApiResponse.ok("Projects", projectAccess.visibleProjects(currentUsers.requireUser(authorization)).stream()
                .filter(project -> project.getAcademicYear().equals(year))
                .toList());
    }

    @GetMapping("/search")
    ApiResponse<?> search(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String keyword
    ) {
        String value = keyword.toLowerCase(Locale.ROOT);
        return ApiResponse.ok("Projects", projectAccess.visibleProjects(currentUsers.requireUser(authorization)).stream()
                .filter(project -> project.getTitle().toLowerCase(Locale.ROOT).contains(value)
                        || project.getProjectNumber().toLowerCase(Locale.ROOT).contains(value))
                .toList());
    }

    @PostMapping("/{projectId}/supervisor/{supervisorId}")
    ApiResponse<?> assignSupervisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID supervisorId
    ) {
        currentUsers.requireAdmin(authorization);
        var project = projects.findById(projectId)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        var supervisor = evaluatorProfiles.findById(supervisorId)
                .orElseThrow(() -> new BusinessException("SUPERVISOR_NOT_FOUND", "Supervisor profile not found"));
        if (supervisor.getUser().getRole() != UserRole.SUPERVISOR) {
            throw new BusinessException("INVALID_SUPERVISOR_ROLE", "Only a supervisor account can supervise a project");
        }
        var existingAssignment = supervisors.findByProjectIdAndSupervisorId(projectId, supervisorId);
        if ((existingAssignment.isEmpty() || !existingAssignment.get().isActive())
                && supervisors.findAllByProjectIdAndActiveTrue(projectId).size() >= 2) {
            throw new BusinessException("SUPERVISOR_LIMIT_EXCEEDED", "A project can have at most two supervisors");
        }
        ProjectSupervisorAssignment assignment = existingAssignment.orElse(new ProjectSupervisorAssignment());
        assignment.setProject(project);
        assignment.setSupervisor(supervisor);
        assignment.setActive(true);
        return ApiResponse.ok("Supervisor assigned", supervisors.save(assignment));
    }

    @GetMapping("/{projectId}/supervisor")
    ApiResponse<?> supervisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        projectAccess.assertCanView(currentUsers.requireUser(authorization), projectId);
        return ApiResponse.ok("Supervisors", supervisors.findAllByProjectIdAndActiveTrue(projectId));
    }

    @DeleteMapping("/{projectId}/supervisor")
    ApiResponse<Void> removeSupervisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        currentUsers.requireAdmin(authorization);
        supervisors.findAllByProjectIdAndActiveTrue(projectId).forEach(assignment -> {
            assignment.setActive(false);
            supervisors.save(assignment);
        });
        return ApiResponse.ok("Supervisor removed", null);
    }

    @PostMapping("/{projectId}/evaluators")
    ApiResponse<?> assignEvaluator(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @Valid @RequestBody EvaluatorAssignmentRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        var project = projects.findById(projectId)
                .orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found"));
        var evaluator = evaluatorProfiles.findById(request.evaluatorId())
                .orElseThrow(() -> new BusinessException("EVALUATOR_NOT_FOUND", "Evaluator not found"));
        assertEvaluationRole(evaluator.getUser().getRole(), request.evaluationType());
        ProjectEvaluatorAssignment assignment = evaluators
                .findByProjectIdAndEvaluatorIdAndEvaluationType(projectId, request.evaluatorId(), request.evaluationType())
                .orElse(new ProjectEvaluatorAssignment());
        assignment.setProject(project);
        assignment.setEvaluator(evaluator);
        assignment.setEvaluationType(request.evaluationType());
        assignment.setActive(true);
        return ApiResponse.ok("Evaluator assigned", evaluators.save(assignment));
    }

    @GetMapping("/{projectId}/evaluators")
    ApiResponse<?> projectEvaluators(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        projectAccess.assertCanView(currentUsers.requireUser(authorization), projectId);
        return ApiResponse.ok("Evaluators", evaluators.findByProjectIdAndActiveTrue(projectId));
    }

    @GetMapping("/my-evaluation-assignments")
    ApiResponse<?> myEvaluationAssignments(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(
                "Evaluation assignments",
                projectAccess.evaluationAssignments(currentUsers.requireUser(authorization))
        );
    }

    @DeleteMapping("/{projectId}/supervisor/{assignmentId}")
    ApiResponse<Void> removeOneSupervisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID assignmentId
    ) {
        currentUsers.requireAdmin(authorization);
        ProjectSupervisorAssignment assignment = supervisors.findById(assignmentId)
                .filter(value -> value.getProject().getId().equals(projectId))
                .orElseThrow(() -> new BusinessException("ASSIGNMENT_NOT_FOUND", "Supervisor assignment not found"));
        assignment.setActive(false);
        supervisors.save(assignment);
        return ApiResponse.ok("Supervisor removed", null);
    }

    @DeleteMapping("/{projectId}/evaluators/{assignmentId}")
    ApiResponse<Void> removeEvaluator(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID assignmentId
    ) {
        currentUsers.requireAdmin(authorization);
        ProjectEvaluatorAssignment assignment = evaluators.findById(assignmentId)
                .filter(value -> value.getProject().getId().equals(projectId))
                .orElseThrow(() -> new BusinessException("ASSIGNMENT_NOT_FOUND", "Evaluator assignment not found"));
        assignment.setActive(false);
        evaluators.save(assignment);
        return ApiResponse.ok("Evaluator removed", null);
    }

    private void assertEvaluationRole(UserRole role, EvaluationType type) {
        boolean valid = switch (role) {
            case INDUSTRY_REPRESENTATIVE -> type == EvaluationType.DEMO_DAY_INDUSTRY;
            case FACULTY_EVALUATOR -> type == EvaluationType.REPORT_PHASE_I
                    || type == EvaluationType.ORAL_PHASE_I
                    || type == EvaluationType.REPORT_PHASE_II
                    || type == EvaluationType.ORAL_PHASE_II;
            case SUPERVISOR -> type == EvaluationType.SUPERVISOR_PHASE_I
                    || type == EvaluationType.SUPERVISOR_PHASE_II;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException("EVALUATOR_ROLE_MISMATCH", "The selected account role cannot complete this evaluation form");
        }
    }

    private void apply(Project project, ProjectRequest request) {
        project.setProjectNumber(request.projectNumber().trim().toUpperCase(Locale.ROOT));
        project.setTitle(request.title().trim());
        project.setAbstractText(request.abstractText());
        project.setAcademicYear(request.academicYear().trim());
        project.setStatus(request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status());
        project.setTrack(tracks.findById(request.trackId())
                .orElseThrow(() -> new BusinessException("TRACK_NOT_FOUND", "Track not found")));
    }

    private void assertProjectNumberAvailable(String projectNumber, UUID currentId) {
        projects.findByProjectNumberIgnoreCase(projectNumber.trim()).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new BusinessException("DUPLICATE_PROJECT_NUMBER", "Project number already exists");
            }
        });
    }
}