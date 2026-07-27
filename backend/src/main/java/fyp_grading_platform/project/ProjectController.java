package fyp_grading_platform.project;

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

    public ProjectController(
            ProjectRepository projects,
            TrackRepository tracks,
            ProjectSupervisorAssignmentRepository supervisors,
            ProjectEvaluatorAssignmentRepository evaluators,
            EvaluatorProfileRepository evaluatorProfiles,
            CurrentUserService currentUsers
    ) {
        this.projects = projects;
        this.tracks = tracks;
        this.supervisors = supervisors;
        this.evaluators = evaluators;
        this.evaluatorProfiles = evaluatorProfiles;
        this.currentUsers = currentUsers;
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
    ApiResponse<?> all() {
        return ApiResponse.ok("Projects", projects.findAll());
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(@PathVariable UUID id) {
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
    ApiResponse<?> byTrack(@PathVariable UUID trackId) {
        return ApiResponse.ok("Projects", projects.findByTrackId(trackId));
    }

    @GetMapping("/by-academic-year/{year}")
    ApiResponse<?> byYear(@PathVariable String year) {
        return ApiResponse.ok("Projects", projects.findByAcademicYear(year));
    }

    @GetMapping("/search")
    ApiResponse<?> search(@RequestParam String keyword) {
        return ApiResponse.ok(
                "Projects",
                projects.findByTitleContainingIgnoreCaseOrProjectNumberContainingIgnoreCase(keyword, keyword)
        );
    }

    @PostMapping("/{projectId}/supervisor/{supervisorId}")
    ApiResponse<?> assignSupervisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID supervisorId
    ) {
        currentUsers.requireAdmin(authorization);
        ProjectSupervisorAssignment assignment = supervisors.findByProjectIdAndActiveTrue(projectId)
                .orElse(new ProjectSupervisorAssignment());
        assignment.setProject(projects.findById(projectId).orElseThrow());
        assignment.setSupervisor(evaluatorProfiles.findById(supervisorId).orElseThrow());
        assignment.setActive(true);
        return ApiResponse.ok("Supervisor assigned", supervisors.save(assignment));
    }

    @GetMapping("/{projectId}/supervisor")
    ApiResponse<?> supervisor(@PathVariable UUID projectId) {
        return ApiResponse.ok("Supervisor", supervisors.findByProjectIdAndActiveTrue(projectId));
    }

    @DeleteMapping("/{projectId}/supervisor")
    ApiResponse<Void> removeSupervisor(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        currentUsers.requireAdmin(authorization);
        supervisors.findByProjectIdAndActiveTrue(projectId).ifPresent(assignment -> {
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
        ProjectEvaluatorAssignment assignment = new ProjectEvaluatorAssignment();
        assignment.setProject(projects.findById(projectId).orElseThrow());
        assignment.setEvaluator(evaluatorProfiles.findById(request.evaluatorId()).orElseThrow());
        assignment.setEvaluationType(request.evaluationType());
        return ApiResponse.ok("Evaluator assigned", evaluators.save(assignment));
    }

    @GetMapping("/{projectId}/evaluators")
    ApiResponse<?> projectEvaluators(@PathVariable UUID projectId) {
        return ApiResponse.ok("Evaluators", evaluators.findByProjectIdAndActiveTrue(projectId));
    }

    @DeleteMapping("/{projectId}/evaluators/{assignmentId}")
    ApiResponse<Void> removeEvaluator(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID assignmentId
    ) {
        currentUsers.requireAdmin(authorization);
        if (!evaluators.existsById(assignmentId)) {
            throw new BusinessException("ASSIGNMENT_NOT_FOUND", "Evaluator assignment not found");
        }
        evaluators.deleteById(assignmentId);
        return ApiResponse.ok("Evaluator removed", null);
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