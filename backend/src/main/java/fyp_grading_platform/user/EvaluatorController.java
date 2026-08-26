package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/evaluators")
public class EvaluatorController {
    private final EvaluatorProfileRepository repository;
    private final UserRepository users;
    private final CurrentUserService currentUsers;

    public EvaluatorController(
            EvaluatorProfileRepository repository,
            UserRepository users,
            CurrentUserService currentUsers
    ) {
        this.repository = repository;
        this.users = users;
        this.currentUsers = currentUsers;
    }

    @PostMapping ApiResponse<EvaluatorProfile> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody EvaluatorRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        EvaluatorProfile profile = new EvaluatorProfile();
        profile.setUser(users.findById(request.userId()).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found")));
        profile.setDepartment(request.department()); profile.setSpecialization(request.specialization()); profile.setExternalOrganization(request.externalOrganization()); profile.setExternal(request.external());
        return ApiResponse.ok("Evaluator created", repository.save(profile));
    }
    @GetMapping ApiResponse<?> all(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Evaluators", repository.findAll());
    }

    @GetMapping("/me") ApiResponse<?> me(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User actor = currentUsers.requireAnyRole(
                authorization,
                UserRole.SUPERVISOR,
                UserRole.REPORT_EVALUATOR,
                UserRole.FACULTY_EVALUATOR,
                UserRole.INDUSTRY_REPRESENTATIVE
        );
        return ApiResponse.ok("Current evaluator", repository.findByUserId(actor.getId())
                .orElseThrow(() -> new BusinessException("EVALUATOR_PROFILE_NOT_FOUND", "Evaluator profile not found")));
    }

    @GetMapping("/{id}") ApiResponse<?> one(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Evaluator", repository.findById(id));
    }

    @PutMapping("/{id}") ApiResponse<?> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody EvaluatorRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        EvaluatorProfile profile = repository.findById(id).orElseThrow(() -> new BusinessException("EVALUATOR_NOT_FOUND", "Evaluator not found"));
        profile.setDepartment(request.department()); profile.setSpecialization(request.specialization()); profile.setExternalOrganization(request.externalOrganization()); profile.setExternal(request.external());
        return ApiResponse.ok("Evaluator updated", repository.save(profile));
    }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        repository.deleteById(id);
        return ApiResponse.ok("Evaluator deleted", null);
    }

    @GetMapping("/internal") ApiResponse<?> internal(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Internal evaluators", repository.findByExternal(false));
    }

    @GetMapping("/external") ApiResponse<?> external(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("External evaluators", repository.findByExternal(true));
    }

    @GetMapping("/available") ApiResponse<?> available(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Available evaluators", repository.findAll());
    }
}
