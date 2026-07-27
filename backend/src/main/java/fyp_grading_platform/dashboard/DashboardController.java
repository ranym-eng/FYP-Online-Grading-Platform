package fyp_grading_platform.dashboard;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.evaluation.EvaluationSubmissionRepository;
import fyp_grading_platform.grading.GradeRepository;
import fyp_grading_platform.project.PhaseRepository;
import fyp_grading_platform.project.ProjectRepository;
import fyp_grading_platform.project.TrackRepository;
import fyp_grading_platform.user.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final UserRepository users;
    private final TrackRepository tracks;
    private final ProjectRepository projects;
    private final PhaseRepository phases;
    private final EvaluationSubmissionRepository submissions;
    private final GradeRepository grades;

    public DashboardController(UserRepository users, TrackRepository tracks, ProjectRepository projects, PhaseRepository phases, EvaluationSubmissionRepository submissions, GradeRepository grades) {
        this.users = users; this.tracks = tracks; this.projects = projects; this.phases = phases; this.submissions = submissions; this.grades = grades;
    }

    @GetMapping("/admin/summary") ApiResponse<?> adminSummary() { return ApiResponse.ok("Admin summary", Map.of("users", users.count(), "tracks", tracks.count(), "projects", projects.count(), "phases", phases.count(), "evaluations", submissions.count(), "grades", grades.count())); }
    @GetMapping("/admin/tracks-status") ApiResponse<?> tracksStatus() { return ApiResponse.ok("Tracks status", tracks.findAll()); }
    @GetMapping("/admin/phases-status") ApiResponse<?> phasesStatus() { return ApiResponse.ok("Phases status", phases.findAll()); }
    @GetMapping("/admin/evaluation-completion") ApiResponse<?> completion() { long total = submissions.count(); long locked = submissions.findAll().stream().filter(s -> s.isLocked()).count(); return ApiResponse.ok("Evaluation completion", Map.of("total", total, "locked", locked)); }
    @GetMapping("/admin/pending-evaluations") ApiResponse<?> pending() { return ApiResponse.ok("Pending evaluations", submissions.findAll().stream().filter(s -> !s.isLocked()).toList()); }
    @GetMapping("/student/me/project") ApiResponse<?> studentProject() { return ApiResponse.ok("Student project placeholder", null); }
    @GetMapping("/student/me/team") ApiResponse<?> studentTeam() { return ApiResponse.ok("Student team placeholder", null); }
    @GetMapping("/student/me/progress") ApiResponse<?> studentProgress() { return ApiResponse.ok("Student progress placeholder", null); }
    @GetMapping("/student/me/grades") ApiResponse<?> studentGrades() { return ApiResponse.ok("Student grades placeholder", grades.findAll().stream().filter(g -> g.isPublished()).toList()); }
    @GetMapping("/evaluator/me/projects") ApiResponse<?> evaluatorProjects() { return ApiResponse.ok("Evaluator projects placeholder", projects.findAll()); }
    @GetMapping("/evaluator/me/pending-evaluations") ApiResponse<?> evaluatorPending() { return pending(); }
    @GetMapping("/evaluator/me/submitted-evaluations") ApiResponse<?> evaluatorSubmitted() { return ApiResponse.ok("Submitted evaluations", submissions.findAll().stream().filter(s -> s.isLocked()).toList()); }
    @GetMapping("/evaluator/me/deadlines") ApiResponse<?> evaluatorDeadlines() { return ApiResponse.ok("Deadlines", phases.findAll()); }
}
