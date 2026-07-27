package fyp_grading_platform.grading;

import fyp_grading_platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
public class GradeController {
    private final GradingService service;
    private final GradeRepository grades;
    private final GradeRuleRepository rules;

    public GradeController(GradingService service, GradeRepository grades, GradeRuleRepository rules) { this.service = service; this.grades = grades; this.rules = rules; }

    @PostMapping("/calculate/project/{projectId}/phase/{phaseId}") ApiResponse<?> calculate(@PathVariable UUID projectId, @PathVariable UUID phaseId) { return ApiResponse.ok("Grade calculated", service.calculate(projectId, phaseId)); }
    @PostMapping("/recalculate/project/{projectId}/phase/{phaseId}") ApiResponse<?> recalculate(@PathVariable UUID projectId, @PathVariable UUID phaseId) { return calculate(projectId, phaseId); }
    @GetMapping("/project/{projectId}") ApiResponse<?> byProject(@PathVariable UUID projectId) { return ApiResponse.ok("Grades", grades.findByProjectId(projectId)); }
    @GetMapping("/project/{projectId}/phase/{phaseId}") ApiResponse<?> byPhase(@PathVariable UUID projectId, @PathVariable UUID phaseId) { return ApiResponse.ok("Grade", grades.findByProjectIdAndPhaseId(projectId, phaseId)); }
    @PatchMapping("/{gradeId}/publish") ApiResponse<?> publish(@PathVariable UUID gradeId) { Grade g = grades.findById(gradeId).orElseThrow(); g.setPublished(true); return ApiResponse.ok("Grade published", grades.save(g)); }
    @PatchMapping("/project/{projectId}/publish") ApiResponse<?> publishProject(@PathVariable UUID projectId) { var list = grades.findByProjectId(projectId); list.forEach(g -> g.setPublished(true)); return ApiResponse.ok("Project grades published", grades.saveAll(list)); }
    @PatchMapping("/project/{projectId}/unpublish") ApiResponse<?> unpublishProject(@PathVariable UUID projectId) { var list = grades.findByProjectId(projectId); list.forEach(g -> g.setPublished(false)); return ApiResponse.ok("Project grades unpublished", grades.saveAll(list)); }
    @GetMapping("/rules") ApiResponse<?> rules() { return ApiResponse.ok("Grade rules", rules.findAll()); }
    @PutMapping("/rules/{id}") ApiResponse<?> updateRule(@PathVariable UUID id, @RequestParam double weight) { GradeRule r = rules.findById(id).orElseThrow(); r.setWeight(weight); return ApiResponse.ok("Rule updated", rules.save(r)); }
}
