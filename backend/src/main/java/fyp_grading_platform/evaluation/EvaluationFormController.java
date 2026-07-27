package fyp_grading_platform.evaluation;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseType;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/evaluation-forms")
public class EvaluationFormController {
    private final EvaluationFormTemplateRepository forms;
    private final RubricCriterionRepository criteria;

    public EvaluationFormController(EvaluationFormTemplateRepository forms, RubricCriterionRepository criteria) { this.forms = forms; this.criteria = criteria; }

    @PostMapping ApiResponse<EvaluationFormTemplate> create(@Valid @RequestBody EvaluationFormRequest request) { EvaluationFormTemplate f = new EvaluationFormTemplate(); apply(f, request); return ApiResponse.ok("Evaluation form created", forms.save(f)); }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Evaluation forms", forms.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Evaluation form", forms.findById(id)); }
    @PutMapping("/{id}") ApiResponse<EvaluationFormTemplate> update(@PathVariable UUID id, @Valid @RequestBody EvaluationFormRequest request) { EvaluationFormTemplate f = forms.findById(id).orElseThrow(() -> new BusinessException("FORM_NOT_FOUND", "Form not found")); apply(f, request); return ApiResponse.ok("Evaluation form updated", forms.save(f)); }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { forms.deleteById(id); return ApiResponse.ok("Evaluation form deleted", null); }
    @GetMapping("/by-type/{type}") ApiResponse<?> byType(@PathVariable EvaluationType type) { return ApiResponse.ok("Forms", forms.findByEvaluationType(type)); }
    @GetMapping("/by-phase/{phaseType}") ApiResponse<?> byPhase(@PathVariable PhaseType phaseType) { return ApiResponse.ok("Forms", forms.findByPhaseType(phaseType)); }
    @PatchMapping("/{id}/activate") ApiResponse<?> activate(@PathVariable UUID id) { EvaluationFormTemplate f = forms.findById(id).orElseThrow(); f.setActive(true); return ApiResponse.ok("Form activated", forms.save(f)); }
    @PatchMapping("/{id}/deactivate") ApiResponse<?> deactivate(@PathVariable UUID id) { EvaluationFormTemplate f = forms.findById(id).orElseThrow(); f.setActive(false); return ApiResponse.ok("Form deactivated", forms.save(f)); }

    @PostMapping("/{formId}/criteria") ApiResponse<RubricCriterion> createCriterion(@PathVariable UUID formId, @Valid @RequestBody CriterionRequest request) { RubricCriterion c = new RubricCriterion(); c.setFormTemplate(forms.findById(formId).orElseThrow()); apply(c, request); return ApiResponse.ok("Criterion created", criteria.save(c)); }
    @GetMapping("/{formId}/criteria") ApiResponse<?> formCriteria(@PathVariable UUID formId) { return ApiResponse.ok("Criteria", criteria.findByFormTemplateIdOrderByDisplayOrderAsc(formId)); }

    private void apply(EvaluationFormTemplate f, EvaluationFormRequest request) { f.setName(request.name()); f.setEvaluationType(request.evaluationType()); f.setPhaseType(request.phaseType()); f.setDescription(request.description()); f.setTotalWeight(request.totalWeight() <= 0 ? 100 : request.totalWeight()); }
    private void apply(RubricCriterion c, CriterionRequest request) { c.setTitle(request.title()); c.setDescription(request.description()); c.setMaxScore(request.maxScore() <= 0 ? 100 : request.maxScore()); c.setWeight(request.weight() <= 0 ? 1 : request.weight()); c.setDisplayOrder(request.displayOrder()); c.setRequired(request.required()); }
}
