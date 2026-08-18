package fyp_grading_platform.project;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.EvaluatorProfile;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectAccessServiceTest {
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final EvaluatorProfileRepository profiles = mock(EvaluatorProfileRepository.class);
    private final ProjectEvaluatorAssignmentRepository evaluatorAssignments = mock(ProjectEvaluatorAssignmentRepository.class);
    private final ProjectSupervisorAssignmentRepository supervisorAssignments = mock(ProjectSupervisorAssignmentRepository.class);
    private final ProjectAccessService access = new ProjectAccessService(
            projects,
            profiles,
            evaluatorAssignments,
            supervisorAssignments
    );

    @Test
    void industryRepresentativeSeesOnlyActivelyAssignedProjects() {
        User actor = user(UserRole.INDUSTRY_REPRESENTATIVE);
        EvaluatorProfile profile = profile(actor);
        Project assigned = project();
        Project incorrectlyAssigned = project();
        ProjectEvaluatorAssignment assignment = evaluatorAssignment(profile, assigned, EvaluationType.DEMO_DAY_INDUSTRY);
        ProjectEvaluatorAssignment invalidAssignment = evaluatorAssignment(profile, incorrectlyAssigned, EvaluationType.ORAL_PHASE_II);
        when(profiles.findByUserId(actor.getId())).thenReturn(Optional.of(profile));
        when(evaluatorAssignments.findByEvaluatorIdAndActiveTrue(profile.getId())).thenReturn(List.of(assignment, invalidAssignment));

        List<Project> visible = access.visibleProjects(actor);

        assertEquals(List.of(assigned), visible);
        assertEquals(
                List.of(new ProjectEvaluationAccess(assigned.getId(), EvaluationType.DEMO_DAY_INDUSTRY)),
                access.evaluationAssignments(actor)
        );
    }

    @Test
    void supervisorSeesSupervisedAndEvaluationAssignedProjectsWithoutDuplicates() {
        User actor = user(UserRole.SUPERVISOR);
        EvaluatorProfile profile = profile(actor);
        Project supervised = project();
        Project evaluated = project();
        ProjectSupervisorAssignment supervision = new ProjectSupervisorAssignment();
        supervision.setProject(supervised);
        supervision.setSupervisor(profile);
        ProjectEvaluatorAssignment duplicate = evaluatorAssignment(profile, supervised, EvaluationType.SUPERVISOR_PHASE_I);
        ProjectEvaluatorAssignment second = evaluatorAssignment(profile, evaluated, EvaluationType.SUPERVISOR_PHASE_II);
        when(profiles.findByUserId(actor.getId())).thenReturn(Optional.of(profile));
        when(supervisorAssignments.findBySupervisorIdAndActiveTrue(profile.getId())).thenReturn(List.of(supervision));
        when(evaluatorAssignments.findByEvaluatorIdAndActiveTrue(profile.getId())).thenReturn(List.of(duplicate, second));

        assertEquals(List.of(supervised, evaluated), access.visibleProjects(actor));
    }

    @Test
    void directAccessToUnassignedProjectIsDenied() {
        User actor = user(UserRole.FACULTY_EVALUATOR);
        Project foreignProject = project();
        when(projects.existsById(foreignProject.getId())).thenReturn(true);
        when(profiles.findByUserId(actor.getId())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> access.assertCanView(actor, foreignProject.getId())
        );

        assertEquals("PROJECT_ACCESS_DENIED", exception.getErrorCode());
    }

    private User user(UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    private EvaluatorProfile profile(User user) {
        EvaluatorProfile profile = new EvaluatorProfile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        return profile;
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        return project;
    }

    private ProjectEvaluatorAssignment evaluatorAssignment(
            EvaluatorProfile evaluator,
            Project project,
            EvaluationType type
    ) {
        ProjectEvaluatorAssignment assignment = new ProjectEvaluatorAssignment();
        assignment.setEvaluator(evaluator);
        assignment.setProject(project);
        assignment.setEvaluationType(type);
        assignment.setActive(true);
        return assignment;
    }
}
