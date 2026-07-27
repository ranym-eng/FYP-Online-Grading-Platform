package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import java.util.UUID;

public record LoginResponse(String token, UUID userId, String email, UserRole role, String fullName) {}
