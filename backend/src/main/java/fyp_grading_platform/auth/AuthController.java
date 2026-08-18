package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.security.TokenService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final CurrentUserService currentUsers;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailDeliveryService emails;

    public AuthController(
            UserRepository users,
            PasswordEncoder encoder,
            TokenService tokens,
            CurrentUserService currentUsers,
            PasswordResetTokenRepository resetTokens,
            EmailDeliveryService emails
    ) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.currentUsers = currentUsers;
        this.resetTokens = resetTokens;
        this.emails = emails;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() != UserStatus.ACTIVE || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return ApiResponse.ok("Login successful", response(user, tokens.generate(user)));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        currentUsers.requireUser(authorization);
        return ApiResponse.ok("Logout successful", null);
    }

    @PostMapping("/refresh-token")
    ApiResponse<LoginResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        User user = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Token refreshed", response(user, tokens.generate(user)));
    }

    @GetMapping("/me")
    ApiResponse<AuthUserResponse> me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        User user = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Current user", new AuthUserResponse(
                user.getId(), user.getUniversityId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole()
        ));
    }

    @GetMapping("/validate-token")
    ApiResponse<TokenValidationResponse> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        User user = currentUsers.requireUser(authorization);
        TokenService.TokenClaims claims = tokens.parse(bearer(authorization));
        return ApiResponse.ok("Token is valid", new TokenValidationResponse(true, user.getId(), user.getRole(), claims.expiresAt()));
    }

    @PostMapping("/change-password")
    ApiResponse<Void> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = currentUsers.requireUser(authorization);
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("PASSWORD_UNCHANGED", "New password must be different");
        }
        user.setPasswordHash(encoder.encode(request.newPassword()));
        users.save(user);
        return ApiResponse.ok("Password changed", null);
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        users.findByEmailIgnoreCase(request.email()).filter(user -> user.getStatus() == UserStatus.ACTIVE).ifPresent(user -> {
            resetTokens.deleteByUserId(user.getId());
            String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
            PasswordResetToken reset = new PasswordResetToken();
            reset.setUser(user);
            reset.setTokenHash(hash(rawToken));
            reset.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            resetTokens.save(reset);
            emails.send(user.getEmail(), "FYP platform password reset",
                    "Use this one-time token within 30 minutes:\n\n" + rawToken, null);
        });
        return ApiResponse.ok("If the account exists, a reset email has been sent", null);
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken reset = resetTokens.findByTokenHashAndUsedAtIsNull(hash(request.token()))
                .orElseThrow(() -> new BusinessException("INVALID_RESET_TOKEN", "Password reset token is invalid"));
        if (reset.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("RESET_TOKEN_EXPIRED", "Password reset token has expired");
        }
        User user = reset.getUser();
        user.setPasswordHash(encoder.encode(request.newPassword()));
        users.save(user);
        reset.setUsedAt(LocalDateTime.now());
        resetTokens.save(reset);
        return ApiResponse.ok("Password reset successful", null);
    }

    private LoginResponse response(User user, String token) {
        return new LoginResponse(token, user.getId(), user.getEmail(), user.getRole(), user.getFullName());
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
    }

    private String bearer(String authorization) {
        return authorization.substring(7).trim();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash reset token", exception);
        }
    }
}

record AuthUserResponse(UUID id, String universityId, String fullName, String email, String phone, UserRole role) {}
record TokenValidationResponse(boolean valid, UUID userId, UserRole role, long expiresAt) {}
record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
record ForgotPasswordRequest(@NotBlank @Email String email) {}
record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}