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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
    private final OneTimeTokenHasher tokenHasher;
    private final SsoLoginService sso;
    private final IndustryInvitationService industryInvitations;
    private final boolean localInternalLoginEnabled;

    public AuthController(
            UserRepository users,
            PasswordEncoder encoder,
            TokenService tokens,
            CurrentUserService currentUsers,
            PasswordResetTokenRepository resetTokens,
            EmailDeliveryService emails,
            OneTimeTokenHasher tokenHasher,
            SsoLoginService sso,
            IndustryInvitationService industryInvitations,
            @Value("${app.auth.local-internal-login-enabled:false}") boolean localInternalLoginEnabled
    ) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.currentUsers = currentUsers;
        this.resetTokens = resetTokens;
        this.emails = emails;
        this.tokenHasher = tokenHasher;
        this.sso = sso;
        this.industryInvitations = industryInvitations;
        this.localInternalLoginEnabled = localInternalLoginEnabled;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() == UserStatus.PENDING_INVITATION) {
            throw new BusinessException("INVITATION_REQUIRED", "Activate the Industry Guest invitation before signing in");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidCredentials();
        }
        if (user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE
                && (user.getAccessExpiresAt() == null || user.getAccessExpiresAt().isBefore(LocalDateTime.now()))) {
            user.setStatus(UserStatus.INACTIVE);
            users.save(user);
            throw new BusinessException("ACCESS_EXPIRED", "Industry Guest access has expired");
        }
        if (user.getRole() != UserRole.INDUSTRY_REPRESENTATIVE && !localInternalLoginEnabled) {
            throw new BusinessException("USE_SQU_SSO", "Use the SQU institutional sign-in button");
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return ApiResponse.ok("Login successful", response(user, tokens.generate(user)));
    }

    @GetMapping("/sso/config")
    ApiResponse<SsoLoginService.SsoConfiguration> ssoConfiguration() {
        return ApiResponse.ok("SQU SSO configuration", sso.configuration());
    }

    @PostMapping("/sso/exchange")
    ApiResponse<LoginResponse> exchangeSsoCode(@Valid @RequestBody SsoExchangeRequest request) {
        User user = sso.exchange(request.code());
        return ApiResponse.ok("SQU login successful", response(user, tokens.generate(user)));
    }

    @PostMapping("/industry/activate")
    ApiResponse<LoginResponse> activateIndustryGuest(@Valid @RequestBody IndustryActivationRequest request) {
        User user = industryInvitations.activate(request.token(), request.newPassword());
        return ApiResponse.ok("Industry Guest account activated", response(user, tokens.generate(user)));
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
                user.getId(), user.getUniversityId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(),
                user.getAccessExpiresAt()
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
        if (user.getRole() != UserRole.INDUSTRY_REPRESENTATIVE && !localInternalLoginEnabled) {
            throw new BusinessException("PASSWORD_MANAGED_BY_SSO", "The password for this account is managed by SQU SSO");
        }
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
        users.findByEmailIgnoreCase(request.email())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE || localInternalLoginEnabled)
                .ifPresent(user -> {
            resetTokens.deleteByUserId(user.getId());
            String rawToken = tokenHasher.generate();
            PasswordResetToken reset = new PasswordResetToken();
            reset.setUser(user);
            reset.setTokenHash(tokenHasher.hash(rawToken));
            reset.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            resetTokens.save(reset);
            emails.send(user.getEmail(), "FYP platform password reset",
                    "Use this one-time token within 30 minutes:\n\n" + rawToken, null);
        });
        return ApiResponse.ok("If the account exists, a reset email has been sent", null);
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken reset = resetTokens.findByTokenHashAndUsedAtIsNull(tokenHasher.hash(request.token()))
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

}

record AuthUserResponse(
        UUID id,
        String universityId,
        String fullName,
        String email,
        String phone,
        UserRole role,
        LocalDateTime accessExpiresAt
) {}
record TokenValidationResponse(boolean valid, UUID userId, UserRole role, long expiresAt) {}
record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
record ForgotPasswordRequest(@NotBlank @Email String email) {}
record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
record SsoExchangeRequest(@NotBlank String code) {}
record IndustryActivationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
