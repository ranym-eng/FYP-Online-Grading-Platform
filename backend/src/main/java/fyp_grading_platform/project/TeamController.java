package fyp_grading_platform.project;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.StudentProfile;
import fyp_grading_platform.user.StudentProfileRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamRepository teams;
    private final ProjectRepository projects;
    private final StudentProfileRepository students;

    public TeamController(TeamRepository teams, ProjectRepository projects, StudentProfileRepository students) { this.teams = teams; this.projects = projects; this.students = students; }

    @PostMapping ApiResponse<Team> create(@Valid @RequestBody TeamRequest request) { Team t = new Team(); apply(t, request); return ApiResponse.ok("Team created", teams.save(t)); }
    @GetMapping ApiResponse<?> all() { return ApiResponse.ok("Teams", teams.findAll()); }
    @GetMapping("/{id}") ApiResponse<?> one(@PathVariable UUID id) { return ApiResponse.ok("Team", teams.findById(id)); }
    @PutMapping("/{id}") ApiResponse<Team> update(@PathVariable UUID id, @Valid @RequestBody TeamRequest request) { Team t = teams.findById(id).orElseThrow(() -> new BusinessException("TEAM_NOT_FOUND", "Team not found")); apply(t, request); return ApiResponse.ok("Team updated", teams.save(t)); }
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable UUID id) { teams.deleteById(id); return ApiResponse.ok("Team deleted", null); }
    @GetMapping("/by-project/{projectId}") ApiResponse<?> byProject(@PathVariable UUID projectId) { return ApiResponse.ok("Team", teams.findByProjectId(projectId)); }
    @GetMapping("/by-academic-year/{year}") ApiResponse<?> byYear(@PathVariable String year) { return ApiResponse.ok("Teams", teams.findByAcademicYear(year)); }
    @PostMapping("/{teamId}/students/{studentId}") ApiResponse<Team> addStudent(@PathVariable UUID teamId, @PathVariable UUID studentId) { Team t = teams.findById(teamId).orElseThrow(); StudentProfile s = students.findById(studentId).orElseThrow(); t.getStudents().add(s); return ApiResponse.ok("Student added", teams.save(t)); }
    @DeleteMapping("/{teamId}/students/{studentId}") ApiResponse<Team> removeStudent(@PathVariable UUID teamId, @PathVariable UUID studentId) { Team t = teams.findById(teamId).orElseThrow(); t.getStudents().removeIf(s -> s.getId().equals(studentId)); return ApiResponse.ok("Student removed", teams.save(t)); }
    @GetMapping("/{teamId}/students") ApiResponse<?> teamStudents(@PathVariable UUID teamId) { return ApiResponse.ok("Team students", teams.findById(teamId).map(Team::getStudents)); }

    private void apply(Team t, TeamRequest request) { t.setName(request.name()); t.setSection(request.section()); t.setAcademicYear(request.academicYear()); t.setProject(projects.findById(request.projectId()).orElseThrow()); if (request.studentIds() != null) request.studentIds().forEach(id -> t.getStudents().add(students.findById(id).orElseThrow())); }
}
