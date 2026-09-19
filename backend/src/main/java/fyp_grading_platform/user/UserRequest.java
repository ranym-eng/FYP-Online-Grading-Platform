package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public record UserRequest(
        @NotBlank String universityId,
        @NotBlank String fullName,
        @Email @NotBlank String email,
        String phone,
        @NotNull UserRole role,
        LocalDateTime accessExpiresAt,
        Boolean activateImmediately,
        Set<UserRole> roles
) {
    public UserRequest(
            String universityId,
            String fullName,
            String email,
            String phone,
            UserRole role,
            LocalDateTime accessExpiresAt,
            Boolean activateImmediately
    ) {
        this(universityId, fullName, email, phone, role, accessExpiresAt, activateImmediately, null);
    }

    public Set<UserRole> effectiveRoles() {
        LinkedHashSet<UserRole> effective = new LinkedHashSet<>();
        if (roles != null) effective.addAll(roles);
        effective.add(role);
        return effective;
    }
}
