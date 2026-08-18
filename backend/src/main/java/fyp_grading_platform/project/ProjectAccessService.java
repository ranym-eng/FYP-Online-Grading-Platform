package fyp_grading_platform.project;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.EvaluatorProfile;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectAccessService {
    private final ProjectRepository projects;
    private final EvaluatorProfileRepository evaluatorProfiles;
    private final ProjectEvaluatorAssignmentRepository evaluatorAssignments;
    private final ProjectSupervisorAssignmentRepository supervisorAssignments;

    public ProjectAccessService(
            ProjectRepository projects,
            EvaluatorProfileRepository evaluatorProfiles,
            ProjectEvaluatorAssignmentRepository evaluatorAssignments,
            ProjectSupervisorAssignmentRepository supervisorAssignments
    ) {
        this.projects = projects;
        this.evaluatorProfiles = evaluatorProfiles;
        this.evaluatorAssignments = evaluatorAssignments;
        this.supervisorAssignments = supervisorAssignments;
    }

    public boolean canViewAll(User actor) {
        return actor.getRole() == UserRole.ADMIN || actor.getRole() == UserRole.COORDINATOR;
    }

    public List<Project> visibleProjects(User actor) {
        if (canViewAll(actor)) return projects.findAll();

        EvaluatorProfile profile = evaluatorProfiles.findByUserId(actor.getId()).orElse(null);
        if (profile == null) return List.of();

        Map<UUID, Project> visible = new LinkedHashMap<>();
        if (actor.getRole() == UserRole.SUPERVISOR) {
            supervisorAssignments.findBySupervisorIdAndActiveTrue(profile.getId())
                    .forEach(assignment -> visible.put(assignment.getProject().getId(), assignment.getProject()));
        }
        evaluatorAssignments.findByEvaluatorIdAndActiveTrue(profile.getId()).stream()
                .filter(assignment -> canUseEvaluationType(actor, assignment.getEvaluationType()))
                .forEach(assignment -> visible.put(assignment.getProject().getId(), assignment.getProject()));
        return List.copyOf(visible.values());
    }

    public List<ProjectEvaluationAccess> evaluationAssignments(User actor) {
        if (canViewAll(actor)) {
            return evaluatorAssignments.findAll().stream()
                    .filter(ProjectEvaluatorAssignment::isActive)
                    .map(this::toAccess)
                    .distinct()
                    .toList();
        }
        return evaluatorProfiles.findByUserId(actor.getId())
                .map(profile -> evaluatorAssignments.findByEvaluatorIdAndActiveTrue(profile.getId()).stream()
                        .filter(assignment -> canUseEvaluationType(actor, assignment.getEvaluationType()))
                        .map(this::toAccess)
                        .distinct()
                        .toList())
                .orElseGet(List::of);
    }

    public boolean canView(User actor, UUID projectId) {
        if (canViewAll(actor)) return true;
        EvaluatorProfile profile = evaluatorProfiles.findByUserId(actor.getId()).orElse(null);
        if (profile == null) return false;
        if (actor.getRole() == UserRole.SUPERVISOR
                && supervisorAssignments.existsByProjectIdAndSupervisorIdAndActiveTrue(projectId, profile.getId())) {
            return true;
        }
        if (actor.getRole() == UserRole.INDUSTRY_REPRESENTATIVE) {
            return evaluatorAssignments.existsByProjectIdAndEvaluatorIdAndEvaluationTypeAndActiveTrue(
                    projectId,
                    profile.getId(),
                    EvaluationType.DEMO_DAY_INDUSTRY
            );
        }
        return evaluatorAssignments.existsByProjectIdAndEvaluatorIdAndActiveTrue(projectId, profile.getId());
    }

    public void assertCanView(User actor, UUID projectId) {
        if (!projects.existsById(projectId)) {
            throw new BusinessException("PROJECT_NOT_FOUND", "Project not found");
        }
        if (!canView(actor, projectId)) {
            throw new BusinessException(
                    "PROJECT_ACCESS_DENIED",
                    "This project is not assigned to your account"
            );
        }
    }

    private ProjectEvaluationAccess toAccess(ProjectEvaluatorAssignment assignment) {
        return new ProjectEvaluationAccess(assignment.getProject().getId(), assignment.getEvaluationType());
    }

    private boolean canUseEvaluationType(User actor, EvaluationType evaluationType) {
        return actor.getRole() != UserRole.INDUSTRY_REPRESENTATIVE
                || evaluationType == EvaluationType.DEMO_DAY_INDUSTRY;
    }
}
