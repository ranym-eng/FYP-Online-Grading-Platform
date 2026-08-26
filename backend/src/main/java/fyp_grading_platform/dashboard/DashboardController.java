package fyp_grading_platform.dashboard;

import fyp_grading_platform.common.EvaluationType;
import fyp_grading_platform.common.PhaseStatus;
import fyp_grading_platform.common.SubmissionStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.evaluation.EvaluationSubmission;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.grading.GradeRepository;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.Project;
import fyp_grading_platform.project.ProjectAccessService;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.reporting.ReportRepository;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.user.EvaluatorProfileRepository;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserRepository users;
    private final TrackRepository tracks;
    private final PhaseRepository phases;
    private final EvaluationSubmissionRepository submissions;
    private final GradeRepository grades;
    private final ReportRepository reports;
    private final EvaluatorProfileRepository evaluatorProfiles;
    private final ProjectAccessService projectAccess;
    private final CurrentUserService currentUsers;

    public DashboardController(
            UserRepository users,
            TrackRepository tracks,
            PhaseRepository phases,
            EvaluationSubmissionRepository submissions,
            GradeRepository grades,
            ReportRepository reports,
            EvaluatorProfileRepository evaluatorProfiles,
            ProjectAccessService projectAccess,
            CurrentUserService currentUsers
    ) {
        this.users = users;
        this.tracks = tracks;
        this.phases = phases;
        this.submissions = submissions;
        this.grades = grades;
        this.reports = reports;
        this.evaluatorProfiles = evaluatorProfiles;
        this.projectAccess = projectAccess;
        this.currentUsers = currentUsers;
    }

    @GetMapping("/me/summary")
    ApiResponse<?> mySummary(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User actor = currentUsers.requireUser(authorization);
        List<Project> visibleProjects = projectAccess.visibleProjects(actor);
        List<EvaluationSubmission> visibleSubmissions = scopedSubmissions(actor);
        long pending = visibleSubmissions.stream().filter(submission -> !submission.isLocked()).count();
        long submitted = visibleSubmissions.size() - pending;
        long visibleGrades = visibleProjects.stream()
                .flatMap(project -> grades.findByProjectId(project.getId()).stream())
                .filter(grade -> projectAccess.canViewAll(actor) || grade.isPublished())
                .count();
        long visibleReports = projectAccess.canViewAll(actor)
                ? visibleProjects.stream().mapToLong(project -> reports.findByProjectId(project.getId()).size()).sum()
                : 0;

        return ApiResponse.ok("Personal dashboard summary", new DashboardSummary(
                actor.getRole(),
                actor.getRole() == UserRole.ADMIN ? users.count() : 0,
                actor.getRole() == UserRole.ADMIN ? tracks.count() : 0,
                visibleProjects.size(),
                phases.count(),
                phases.findByStatus(PhaseStatus.OPEN).size(),
                visibleSubmissions.size(),
                pending,
                submitted,
                visibleGrades,
                visibleReports
        ));
    }

    @GetMapping("/me/pending-evaluations")
    ApiResponse<?> myPendingEvaluations(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        User actor = currentUsers.requireUser(authorization);
        List<DashboardEvaluationItem> pending = scopedSubmissions(actor).stream()
                .filter(submission -> !submission.isLocked())
                .map(this::toItem)
                .toList();
        return ApiResponse.ok("Personal pending evaluations", pending);
    }

    @GetMapping("/admin/summary")
    ApiResponse<?> adminSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAdmin(authorization);
        return mySummary(authorization);
    }

    @GetMapping("/admin/tracks-status")
    ApiResponse<?> tracksStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAnyRole(authorization, UserRole.ADMIN, UserRole.COORDINATOR);
        return ApiResponse.ok("Tracks status", tracks.findAll());
    }

    @GetMapping("/admin/phases-status")
    ApiResponse<?> phasesStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAnyRole(authorization, UserRole.ADMIN, UserRole.COORDINATOR);
        return ApiResponse.ok("Phases status", phases.findAll());
    }

    @GetMapping("/admin/evaluation-completion")
    ApiResponse<?> completion(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAnyRole(authorization, UserRole.ADMIN, UserRole.COORDINATOR);
        long total = submissions.count();
        long locked = submissions.findAll().stream().filter(EvaluationSubmission::isLocked).count();
        return ApiResponse.ok("Evaluation completion", new EvaluationCompletion(total, locked, total - locked));
    }

    @GetMapping("/admin/pending-evaluations")
    ApiResponse<?> adminPending(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        currentUsers.requireAnyRole(authorization, UserRole.ADMIN, UserRole.COORDINATOR);
        return ApiResponse.ok("Pending evaluations", submissions.findAll().stream()
                .filter(submission -> !submission.isLocked())
                .map(this::toItem)
                .toList());
    }

    private List<EvaluationSubmission> scopedSubmissions(User actor) {
        if (projectAccess.canViewAll(actor)) return submissions.findAll();
        return evaluatorProfiles.findByUserId(actor.getId())
                .map(profile -> submissions.findByEvaluatorId(profile.getId()))
                .orElseGet(List::of);
    }

    private DashboardEvaluationItem toItem(EvaluationSubmission submission) {
        return new DashboardEvaluationItem(
                submission.getId(),
                submission.getEvaluationType(),
                submission.getStatus(),
                submission.isLocked(),
                submission.getProject().getProjectNumber(),
                submission.getProject().getTitle(),
                submission.getEvaluator().getUser().getFullName(),
                submission.getUpdatedAt()
        );
    }
}

record DashboardSummary(
        UserRole role,
        long users,
        long tracks,
        long projects,
        long phases,
        long openPhases,
        long evaluations,
        long pendingEvaluations,
        long submittedEvaluations,
        long grades,
        long reports
) {}

record DashboardEvaluationItem(
        UUID id,
        EvaluationType evaluationType,
        SubmissionStatus status,
        boolean locked,
        String project,
        String projectTitle,
        String evaluator,
        LocalDateTime updatedAt
) {}

record EvaluationCompletion(long total, long locked, long pending) {}
