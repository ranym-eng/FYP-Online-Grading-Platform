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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grades")
public class GradeController {
    private final GradingService service;
    private final GradeRepository grades;
    private final StudentPhaseGradeRepository studentGrades;
    private final ConsolidationService consolidation;
    private final GradeRuleRepository rules;
    private final CurrentUserService currentUsers;
    private final ProjectAccessService projectAccess;

    public GradeController(
            GradingService service,
            GradeRepository grades,
            StudentPhaseGradeRepository studentGrades,
            ConsolidationService consolidation,
            GradeRuleRepository rules,
            CurrentUserService currentUsers,
            ProjectAccessService projectAccess
    ) {
        this.service = service;
        this.grades = grades;
        this.studentGrades = studentGrades;
        this.consolidation = consolidation;
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
        return ApiResponse.ok("Project and student grades calculated", service.calculate(projectId, phaseId));
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
        List<Grade> visible = grades.findByProjectId(projectId);
        if (!projectAccess.canViewAll(actor)) {
            visible = visible.stream().filter(Grade::isPublished).toList();
        }
        return ApiResponse.ok("Grades", visible);
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

    @GetMapping("/students/project/{projectId}")
    ApiResponse<?> studentResultsByProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok(
                "Student grades",
                visibleStudentGrades(actor, consolidation.resultsByProject(projectId))
        );
    }

    @GetMapping("/students/project/{projectId}/phase/{phaseId}")
    ApiResponse<?> studentResultsByPhase(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId,
            @PathVariable UUID phaseId
    ) {
        User actor = currentUsers.requireUser(authorization);
        projectAccess.assertCanView(actor, projectId);
        return ApiResponse.ok(
                "Student phase grades",
                visibleStudentGrades(actor, consolidation.results(projectId, phaseId))
        );
    }

    @PatchMapping("/{gradeId}/publish")
    ApiResponse<?> publish(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID gradeId
    ) {
        currentUsers.requireAdmin(authorization);
        Grade grade = grades.findById(gradeId).orElseThrow();
        consolidation.publish(grade.getProject().getId(), grade.getPhase().getId(), true);
        return ApiResponse.ok("Grade published", grades.findById(gradeId).orElseThrow());
    }

    @PatchMapping("/project/{projectId}/publish")
    ApiResponse<?> publishProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        currentUsers.requireAdmin(authorization);
        List<Grade> projectGrades = grades.findByProjectId(projectId);
        projectGrades.forEach(grade -> consolidation.publish(projectId, grade.getPhase().getId(), true));
        return ApiResponse.ok("Project grades published", grades.findByProjectId(projectId));
    }

    @PatchMapping("/project/{projectId}/unpublish")
    ApiResponse<?> unpublishProject(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID projectId
    ) {
        currentUsers.requireAdmin(authorization);
        List<Grade> projectGrades = grades.findByProjectId(projectId);
        projectGrades.forEach(grade -> {
            List<StudentPhaseGrade> values = studentGrades
                    .findByProjectIdAndPhaseIdOrderByStudentStudentNumberAsc(projectId, grade.getPhase().getId());
            if (!values.isEmpty()) consolidation.publish(projectId, grade.getPhase().getId(), false);
        });
        return ApiResponse.ok("Project grades unpublished", grades.findByProjectId(projectId));
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
        if (!Double.isFinite(weight) || weight < 0 || weight > 100) {
            throw new IllegalArgumentException("Weight must be between 0 and 100");
        }
        GradeRule rule = rules.findById(id).orElseThrow();
        rule.setWeight(weight);
        return ApiResponse.ok("Rule updated", rules.save(rule));
    }

    private List<StudentPhaseGrade> visibleStudentGrades(User actor, List<StudentPhaseGrade> values) {
        if (projectAccess.canViewAll(actor)) return values;
        return values.stream().filter(StudentPhaseGrade::isPublished).toList();
    }
}