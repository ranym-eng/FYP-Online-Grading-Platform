package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/criteria")
public class CriterionController {
    private final RubricCriterionRepository repository;
    public CriterionController(RubricCriterionRepository repository) { this.repository = repository; }

    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Criterion", repository.findById(id)); }
    @PutMapping("/{id}") ApiResponse<RubricCriterion> update(@PathVariable UUID id, @Valid @RequestBody CriterionRequest request) { RubricCriterion c = repository.findById(id).orElseThrow(() -> new BusinessException("CRITERION_NOT_FOUND", "Criterion not found")); c.setTitle(request.title()); c.setDescription(request.description()); c.setMaxScore(request.maxScore()); c.setWeight(request.weight()); c.setDisplayOrder(request.displayOrder()); c.setRequired(request.required()); return ApiResponse.ok("Criterion updated", repository.save(c)); }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { repository.deleteById(id); return ApiResponse.ok("Criterion deleted", null); }
    @PatchMapping("/{id}/order") ApiResponse<RubricCriterion> order(@PathVariable UUID id, @RequestParam int displayOrder) { RubricCriterion c = repository.findById(id).orElseThrow(); c.setDisplayOrder(displayOrder); return ApiResponse.ok("Criterion order updated", repository.save(c)); }
}
