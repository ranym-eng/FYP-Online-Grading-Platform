package fyp_grading_platform.user;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/evaluators")
public class EvaluatorController {
    private final EvaluatorProfileRepository repository;
    private final UserRepository users;

    public EvaluatorController(EvaluatorProfileRepository repository, UserRepository users) { this.repository = repository; this.users = users; }

    @PostMapping ApiResponse<EvaluatorProfile> create(@Valid @RequestBody EvaluatorRequest request) {
        EvaluatorProfile profile = new EvaluatorProfile();
        profile.setUser(users.findById(request.userId()).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found")));
        profile.setDepartment(request.department()); profile.setSpecialization(request.specialization()); profile.setExternalOrganization(request.externalOrganization()); profile.setExternal(request.external());
        return ApiResponse.ok("Evaluator created", repository.save(profile));
    }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Evaluators", repository.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Evaluator", repository.findById(id)); }
    @PutMapping("/{id}") ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody EvaluatorRequest request) {
        EvaluatorProfile profile = repository.findById(id).orElseThrow(() -> new BusinessException("EVALUATOR_NOT_FOUND", "Evaluator not found"));
        profile.setDepartment(request.department()); profile.setSpecialization(request.specialization()); profile.setExternalOrganization(request.externalOrganization()); profile.setExternal(request.external());
        return ApiResponse.ok("Evaluator updated", repository.save(profile));
    }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { repository.deleteById(id); return ApiResponse.ok("Evaluator deleted", null); }
    @GetMapping("/internal") ApiResponse<?> internal() { return ApiResponse.ok("Internal evaluators", repository.findByExternal(false)); }
    @GetMapping("/external") ApiResponse<?> external() { return ApiResponse.ok("External evaluators", repository.findByExternal(true)); }
    @GetMapping("/available") ApiResponse<?> available() { return ApiResponse.ok("Available evaluators", repository.findAll()); }
}
