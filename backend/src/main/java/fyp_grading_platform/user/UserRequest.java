package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UserRequest(
        @NotBlank String universityId,
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        String password,
        @NotNull UserRole role,
        LocalDateTime accessExpiresAt
) {}
