package fyp_grading_platform.user;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentProfileRepository repository;

    public StudentController(StudentProfileRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    ApiResponse<StudentProfile> create(@Valid @RequestBody StudentRequest request) {
        if (repository.existsByStudentNumber(request.studentNumber())) {
            throw new BusinessException("DUPLICATE_STUDENT", "Student number already exists");
        }
        StudentProfile profile = new StudentProfile();
        apply(profile, request);
        return ApiResponse.ok("Student created", repository.save(profile));
    }

    @GetMapping
    ApiResponse<?> all() {
        return ApiResponse.ok("Students", repository.findAll());
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(@PathVariable UUID id) {
        return ApiResponse.ok("Student", repository.findById(id));
    }

    @PutMapping("/{id}")
    ApiResponse<?> update(@PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
        StudentProfile profile = repository.findById(id)
                .orElseThrow(() -> new BusinessException("STUDENT_NOT_FOUND", "Student not found"));
        apply(profile, request);
        return ApiResponse.ok("Student updated", repository.save(profile));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ApiResponse.ok("Student deleted", null);
    }

    @GetMapping("/by-track/{trackCode}")
    ApiResponse<?> byTrack(@PathVariable String trackCode) {
        return ApiResponse.ok("Students", repository.findByTrackCode(trackCode));
    }

    private void apply(StudentProfile profile, StudentRequest request) {
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setStudentNumber(request.studentNumber());
        profile.setAcademicYear(request.academicYear());
        profile.setTrackCode(request.trackCode());
        profile.setLevel(request.level());
    }
}
