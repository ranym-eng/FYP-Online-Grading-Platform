package fyp_grading_platform.user;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Official SQU student record enriched with optional FYP metadata")
public record StudentRequest(
        @JsonAlias({"stdID", "studentId"})
        @NotBlank
        @Pattern(regexp = "\\d{5,12}", message = "Student ID must contain 5 to 12 digits")
        String studentNumber,
        @JsonAlias("name")
        @NotBlank
        String fullName,
        @JsonAlias("Email")
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Pattern(regexp = "(?:\\d{2}|\\d{4})", message = "Cohort must use YY or YYYY format")
        String cohort,
        String academicYear,
        String trackCode,
        String level
) {}