package fyp_grading_platform.project;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectRepository projects;
    private final TrackRepository tracks;
    private final ProjectSupervisorAssignmentRepository supervisors;
    private final ProjectEvaluatorAssignmentRepository evaluators;
    private final fyp_grading_platform.user.EvaluatorProfileRepository evaluatorProfiles;

    public ProjectController(ProjectRepository projects, TrackRepository tracks, ProjectSupervisorAssignmentRepository supervisors, ProjectEvaluatorAssignmentRepository evaluators, fyp_grading_platform.user.EvaluatorProfileRepository evaluatorProfiles) {
        this.projects = projects; this.tracks = tracks; this.supervisors = supervisors; this.evaluators = evaluators; this.evaluatorProfiles = evaluatorProfiles;
    }

    @PostMapping ApiResponse<Project> create(@Valid @RequestBody ProjectRequest request) { Project p = new Project(); apply(p, request); return ApiResponse.ok("Project created", projects.save(p)); }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Projects", projects.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Project", projects.findById(id)); }
    @PutMapping("/{id}") ApiResponse<Project> update(@PathVariable UUID id, @Valid @RequestBody ProjectRequest request) { Project p = projects.findById(id).orElseThrow(() -> new BusinessException("PROJECT_NOT_FOUND", "Project not found")); apply(p, request); return ApiResponse.ok("Project updated", projects.save(p)); }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { projects.deleteById(id); return ApiResponse.ok("Project deleted", null); }
    @PatchMapping("/{id}/status") ApiResponse<Project> status(@PathVariable UUID id, @RequestParam String status) { Project p = projects.findById(id).orElseThrow(); p.setStatus(status); return ApiResponse.ok("Project status updated", projects.save(p)); }
    @GetMapping("/by-track/{trackId}") ApiResponse<?> byTrack(@PathVariable UUID trackId) { return ApiResponse.ok("Projects", projects.findByTrackId(trackId)); }
    @GetMapping("/by-academic-year/{year}") ApiResponse<?> byYear(@PathVariable String year) { return ApiResponse.ok("Projects", projects.findByAcademicYear(year)); }
    @GetMapping("/search") ApiResponse<?> search(@RequestParam String keyword) { return ApiResponse.ok("Projects", projects.findByTitleContainingIgnoreCase(keyword)); }

    @PostMapping("/{projectId}/supervisor/{supervisorId}") ApiResponse<?> assignSupervisor(@PathVariable UUID projectId, @PathVariable UUID supervisorId) {
        ProjectSupervisorAssignment a = supervisors.findByProjectIdAndActiveTrue(projectId).orElse(new ProjectSupervisorAssignment());
        a.setProject(projects.findById(projectId).orElseThrow()); a.setSupervisor(evaluatorProfiles.findById(supervisorId).orElseThrow()); a.setActive(true);
        return ApiResponse.ok("Supervisor assigned", supervisors.save(a));
    }
    @GetMapping("/{projectId}/supervisor") ApiResponse<?> supervisor(@PathVariable UUID projectId) { return ApiResponse.ok("Supervisor", supervisors.findByProjectIdAndActiveTrue(projectId)); }
    @DeleteMapping("/{projectId}/supervisor") ApiResponse<Void> removeSupervisor(@PathVariable UUID projectId) { supervisors.findByProjectIdAndActiveTrue(projectId).ifPresent(a -> { a.setActive(false); supervisors.save(a); }); return ApiResponse.ok("Supervisor removed", null); }

    @PostMapping("/{projectId}/evaluators") ApiResponse<?> assignEvaluator(@PathVariable UUID projectId, @Valid @RequestBody EvaluatorAssignmentRequest request) {
        ProjectEvaluatorAssignment a = new ProjectEvaluatorAssignment(); a.setProject(projects.findById(projectId).orElseThrow()); a.setEvaluator(evaluatorProfiles.findById(request.evaluatorId()).orElseThrow()); a.setEvaluationType(request.evaluationType()); return ApiResponse.ok("Evaluator assigned", evaluators.save(a));
    }
    @GetMapping("/{projectId}/evaluators") ApiResponse<?> projectEvaluators(@PathVariable UUID projectId) { return ApiResponse.ok("Evaluators", evaluators.findByProjectIdAndActiveTrue(projectId)); }
    @DeleteMapping("/{projectId}/evaluators/{assignmentId}") ApiResponse<Void> removeEvaluator(@PathVariable UUID assignmentId) { evaluators.deleteById(assignmentId); return ApiResponse.ok("Evaluator removed", null); }

    private void apply(Project p, ProjectRequest request) { p.setTitle(request.title()); p.setAbstractText(request.abstractText()); p.setAcademicYear(request.academicYear()); p.setStatus(request.status() == null ? "ACTIVE" : request.status()); p.setTrack(tracks.findById(request.trackId()).orElseThrow(() -> new BusinessException("TRACK_NOT_FOUND", "Track not found"))); }
}
