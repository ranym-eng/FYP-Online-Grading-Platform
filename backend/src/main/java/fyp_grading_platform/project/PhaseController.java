package fyp_grading_platform.project;

import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/phases")
public class PhaseController {
    private final PhaseRepository phases;
    private final CurrentUserService currentUsers;
    private final PhaseWindowService windows;

    public PhaseController(PhaseRepository phases, CurrentUserService currentUsers, PhaseWindowService windows) {
        this.phases = phases;
        this.currentUsers = currentUsers;
        this.windows = windows;
    }

    @PostMapping
    ApiResponse<Phase> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody PhaseRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        Phase phase = new Phase();
        apply(phase, request);
        return ApiResponse.ok("Phase created", phases.save(phase));
    }

    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Phases", phases.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Phase", phases.findById(id)); }

    @PutMapping("/{id}")
    ApiResponse<Phase> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody PhaseRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        Phase phase = phases.findById(id).orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        apply(phase, request);
        return ApiResponse.ok("Phase updated", phases.save(phase));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        phases.deleteById(id);
        return ApiResponse.ok("Phase deleted", null);
    }

    @PatchMapping("/{id}/open")
    ApiResponse<Phase> open(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID id) {
        currentUsers.requireAdmin(authorization);
        return status(id, PhaseStatus.OPEN, "Phase opened");
    }

    @PatchMapping("/{id}/close")
    ApiResponse<Phase> close(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID id) {
        currentUsers.requireAdmin(authorization);
        return status(id, PhaseStatus.CLOSED, "Phase closed");
    }

    @PatchMapping("/{id}/archive")
    ApiResponse<Phase> archive(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable UUID id) {
        currentUsers.requireAdmin(authorization);
        return status(id, PhaseStatus.ARCHIVED, "Phase archived");
    }

    @GetMapping("/by-academic-year/{year}") ApiResponse<?> byYear(@PathVariable String year) { return ApiResponse.ok("Phases", phases.findByAcademicYear(year)); }
    @GetMapping("/status/{status}") ApiResponse<?> byStatus(@PathVariable PhaseStatus status) { return ApiResponse.ok("Phases", phases.findByStatus(status)); }
    @GetMapping("/current") ApiResponse<?> current() { return ApiResponse.ok("Current phases", phases.findByStatus(PhaseStatus.OPEN)); }

    @GetMapping("/{id}/evaluation-access")
    ApiResponse<?> evaluationAccess(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        Phase phase = phases.findById(id).orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        return ApiResponse.ok("Evaluation access", windows.access(phase, currentUsers.requireUser(authorization)));
    }

    private ApiResponse<Phase> status(UUID id, PhaseStatus status, String message) {
        Phase phase = phases.findById(id).orElseThrow(() -> new BusinessException("PHASE_NOT_FOUND", "Phase not found"));
        phase.setStatus(status);
        return ApiResponse.ok(message, phases.save(phase));
    }

    private void apply(Phase phase, PhaseRequest request) {
        if (!request.deadline().isAfter(request.startDate())) {
            throw new BusinessException("INVALID_PHASE_DATES", "The phase deadline must be after its start date");
        }
        phase.setType(request.type());
        phase.setName(request.name());
        phase.setAcademicYear(request.academicYear());
        phase.setStartDate(request.startDate());
        phase.setDeadline(request.deadline());
        if (request.status() != null) phase.setStatus(request.status());
    }
}