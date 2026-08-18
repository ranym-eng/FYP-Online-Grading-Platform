package fyp_grading_platform.grading;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.project.ProjectAccessService;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
public class GradeController {
    private final GradingService service;
    private final GradeRepository grades;
    private final GradeRuleRepository rules;
    private final CurrentUserService currentUsers;
    private final ProjectAccessService projectAccess;

    public GradeController(
            GradingService service,
            GradeRepository grades,
            GradeRuleRepository rules,
            CurrentUserService currentUsers,
            ProjectAccessService projectAccess
    ) {
        this.service = service;
        this.grades = grades;
        this.rules = rules;
        this.currentUsers = currentUsers;
        this.projectAccess = projectAccess;
    }

    @PostMapping("/calculate/project/{projectId}/phase/{phaseId}")
    ApiResponse<?> calculate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId
    ) {
        currentUsers.requireAdmin(authorization);
        return ApiResponse.ok("Grade calculated", service.calculate(projectId, phaseId));
    }

    @PostMapping("/recalculate/project/{projectId}/phase/{phaseId}")
    ApiResponse<?> recalculate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId
    ) {
        return calculate(authorization, projectId, phaseId);
    }

    @GetMapping("/project/{projectId}")
    ApiResponse<?> byProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        var visibleGrades = grades.findByProjectId(projectId);
        if (!projectAccess.canViewAll(actor)) {
            visibleGrades = visibleGrades.stream().filter(Grade::isPublished).toList();
        }
        return ApiResponse.ok("Grades", visibleGrades);
    }

    @GetMapping("/project/{projectId}/phase/{phaseId}")
    ApiResponse<?> byPhase(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        Grade grade = grades.findByProjectIdAndPhaseId(projectId, phaseId).orElse(null);
        if (grade != null && !projectAccess.canViewAll(actor) && !grade.isPublished()) {
            grade = null;
        }
        return ApiResponse.ok("Grade", grade);
    }

    @PatchMapping("/{gradeId}/publish")
    ApiResponse<?> publish(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID gradeId
    ) {
        currentUsers.requireAdmin(authorization);
        Grade grade = grades.findById(gradeId).orElseThrow();
        grade.setPublished(true);
        return ApiResponse.ok("Grade published", grades.save(grade));
    }

    @PatchMapping("/project/{projectId}/publish")
    ApiResponse<?> publishProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        currentUsers.requireAdmin(authorization);
        var projectGrades = grades.findByProjectId(projectId);
        projectGrades.forEach(grade -> grade.setPublished(true));
        return ApiResponse.ok("Project grades published", grades.saveAll(projectGrades));
    }

    @PatchMapping("/project/{projectId}/unpublish")
    ApiResponse<?> unpublishProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        currentUsers.requireAdmin(authorization);
        var projectGrades = grades.findByProjectId(projectId);
        projectGrades.forEach(grade -> grade.setPublished(false));
        return ApiResponse.ok("Project grades unpublished", grades.saveAll(projectGrades));
    }

    @GetMapping("/rules")
    ApiResponse<?> rules(@RequestHeader(value = "Authorization", required = false) String authorization) {
        currentUsers.requireUser(authorization);
        return ApiResponse.ok("Grade rules", rules.findAll());
    }

    @PutMapping("/rules/{id}")
    ApiResponse<?> updateRule(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @RequestParam double weight
    ) {
        currentUsers.requireAdmin(authorization);
        GradeRule rule = rules.findById(id).orElseThrow();
        rule.setWeight(weight);
        return ApiResponse.ok("Rule updated", rules.save(rule));
    }
}
