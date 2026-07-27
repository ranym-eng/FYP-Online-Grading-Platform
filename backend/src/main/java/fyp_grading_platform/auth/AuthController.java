package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.TokenService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthController(UserRepository users, PasswordEncoder encoder, TokenService tokens) { this.users = users; this.encoder = encoder; this.tokens = tokens; }

    @PostMapping("/login") ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Invalid email or password"));
        if (user.getStatus() != UserStatus.ACTIVE) throw new BusinessException("ACCOUNT_INACTIVE", "Account is inactive");
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        return ApiResponse.ok("Login successful", new LoginResponse(tokens.generate(user), user.getId(), user.getEmail(), user.getRole(), user.getFullName()));
    }

    @PostMapping("/logout") ApiResponse<?> logout() { return ApiResponse.ok("Logout successful", null); }
    @PostMapping("/refresh-token") ApiResponse<?> refresh() { return ApiResponse.ok("Refresh token placeholder", Map.of("token", "refresh-not-configured-yet")); }
    @GetMapping("/me") ApiResponse<?> me() { return ApiResponse.ok("Current user must be resolved from JWT in production", null); }
    @GetMapping("/validate-token") ApiResponse<?> validateToken() { return ApiResponse.ok("Token validation placeholder", true); }
    @PostMapping("/change-password") ApiResponse<?> changePassword() { return ApiResponse.ok("Password changed placeholder", null); }
    @PostMapping("/forgot-password") ApiResponse<?> forgotPassword() { return ApiResponse.ok("Password reset email placeholder", null); }
    @PostMapping("/reset-password") ApiResponse<?> resetPassword() { return ApiResponse.ok("Password reset placeholder", null); }
}
