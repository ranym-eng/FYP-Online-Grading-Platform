package fyp_grading_platform.user;

import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Students", description = "Official SQU student data and FYP enrichment")
public class StudentController {
    private final StudentProfileRepository repository;
    private final CurrentUserService currentUsers;

    public StudentController(StudentProfileRepository repository, CurrentUserService currentUsers) {
        this.repository = repository;
        this.currentUsers = currentUsers;
    }

    @PostMapping
    @Operation(summary = "Add one student manually")
    ApiResponse<StudentProfile> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody StudentRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        String studentNumber = normalizeStudentNumber(request.studentNumber());
        String email = normalizeEmail(request.email());
        assertSquEmail(studentNumber, email);
        if (repository.existsByStudentNumber(studentNumber)) {
            throw new BusinessException("DUPLICATE_STUDENT", "Student ID already exists");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("DUPLICATE_STUDENT_EMAIL", "Student email already exists");
        }
        StudentProfile profile = new StudentProfile();
        apply(profile, request);
        return ApiResponse.ok("Student created", repository.save(profile));
    }

    @GetMapping
    @Operation(summary = "List all students")
    ApiResponse<?> all() {
        return ApiResponse.ok("Students", repository.findAll());
    }

    @GetMapping("/{id}")
    ApiResponse<?> one(@PathVariable UUID id) {
        return ApiResponse.ok("Student", repository.findById(id)
                .orElseThrow(() -> new BusinessException("STUDENT_NOT_FOUND", "Student not found")));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one student")
    ApiResponse<?> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request
    ) {
        currentUsers.requireAdmin(authorization);
        StudentProfile profile = repository.findById(id)
                .orElseThrow(() -> new BusinessException("STUDENT_NOT_FOUND", "Student not found"));
        String studentNumber = normalizeStudentNumber(request.studentNumber());
        String email = normalizeEmail(request.email());
        assertSquEmail(studentNumber, email);
        repository.findByStudentNumber(studentNumber)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new BusinessException("DUPLICATE_STUDENT", "Student ID already exists"); });
        repository.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new BusinessException("DUPLICATE_STUDENT_EMAIL", "Student email already exists"); });
        apply(profile, request);
        return ApiResponse.ok("Student updated", repository.save(profile));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one student")
    ApiResponse<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID id
    ) {
        currentUsers.requireAdmin(authorization);
        if (!repository.existsById(id)) {
            throw new BusinessException("STUDENT_NOT_FOUND", "Student not found");
        }
        repository.deleteById(id);
        return ApiResponse.ok("Student deleted", null);
    }

    @GetMapping("/by-track/{trackCode}")
    ApiResponse<?> byTrack(@PathVariable String trackCode) {
        return ApiResponse.ok("Students", repository.findByTrackCode(trackCode.trim().toUpperCase(Locale.ROOT)));
    }

    @GetMapping("/by-cohort/{cohort}")
    ApiResponse<?> byCohort(@PathVariable String cohort) {
        return ApiResponse.ok("Students", repository.findByCohort(normalizeCohort(cohort)));
    }

    @GetMapping("/search")
    ApiResponse<?> search(@RequestParam String keyword) {
        String value = keyword.trim();
        return ApiResponse.ok(
                "Students",
                repository.findByFullNameContainingIgnoreCaseOrStudentNumberContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        value,
                        value,
                        value
                )
        );
    }

    private void apply(StudentProfile profile, StudentRequest request) {
        String studentNumber = normalizeStudentNumber(request.studentNumber());
        String email = normalizeEmail(request.email());
        assertSquEmail(studentNumber, email);
        profile.setStudentNumber(studentNumber);
        profile.setFullName(normalizeName(request.fullName()));
        profile.setEmail(email);
        profile.setCohort(normalizeCohort(request.cohort()));
        profile.setAcademicYear(blankToNull(request.academicYear()));
        profile.setTrackCode(request.trackCode() == null ? null : request.trackCode().trim().toUpperCase(Locale.ROOT));
        profile.setLevel(blankToNull(request.level()));
    }

    public static String normalizeStudentNumber(String value) {
        return value == null ? "" : value.trim().replaceFirst("^s(?=\\d+$)", "");
    }

    public static String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeCohort(String value) {
        String cohort = value == null ? "" : value.trim().replaceFirst("\\.0$", "");
        if (cohort.matches("\\d{2}")) cohort = "20" + cohort;
        if (!cohort.matches("(?:19|20)\\d{2}")) {
            throw new BusinessException("INVALID_COHORT", "Cohort must use YY or YYYY format");
        }
        return cohort;
    }

    public static void assertSquEmail(String studentNumber, String email) {
        String expected = "s" + studentNumber + "@student.squ.edu.om";
        if (!email.equals(expected)) {
            throw new BusinessException("INVALID_STUDENT_EMAIL", "Expected SQU email: " + expected);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}