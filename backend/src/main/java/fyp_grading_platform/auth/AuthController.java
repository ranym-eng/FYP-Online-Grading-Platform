package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.security.TokenService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final CurrentUserService currentUsers;
    private final PasswordResetService passwordResets;
    private final SignupService signup;
    private final SsoLoginService sso;
    private final IndustryInvitationService industryInvitations;
    private final boolean localInternalLoginEnabled;

    public AuthController(
            UserRepository users,
            PasswordEncoder encoder,
            TokenService tokens,
            CurrentUserService currentUsers,
            PasswordResetService passwordResets,
            SignupService signup,
            SsoLoginService sso,
            IndustryInvitationService industryInvitations,
            @Value("${app.auth.local-internal-login-enabled:false}") boolean localInternalLoginEnabled
    ) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.currentUsers = currentUsers;
        this.passwordResets = passwordResets;
        this.signup = signup;
        this.sso = sso;
        this.industryInvitations = industryInvitations;
        this.localInternalLoginEnabled = localInternalLoginEnabled;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION || user.getStatus() == UserStatus.PENDING_INVITATION) {
            throw new BusinessException("SIGNUP_REQUIRED", "Complete Sign up before signing in");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidCredentials();
        }
        if (user.isIndustryOnly()
                && (user.getAccessExpiresAt() == null || user.getAccessExpiresAt().isBefore(LocalDateTime.now()))) {
            user.setStatus(UserStatus.INACTIVE);
            users.save(user);
            throw new BusinessException("ACCESS_EXPIRED", "Industry Guest access has expired");
        }
        if (!user.isIndustryOnly() && !localInternalLoginEnabled) {
            throw new BusinessException("USE_SQU_SSO", "Use the SQU institutional sign-in button");
        }
        if (user.getPasswordHash() == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.isPasswordChangeRequired()) {
            if (user.getTemporaryPasswordExpiresAt() == null
                    || !user.getTemporaryPasswordExpiresAt().isAfter(LocalDateTime.now())) {
                throw new BusinessException("TEMPORARY_PASSWORD_EXPIRED", "Ask an administrator for a new temporary password");
            }
            return ApiResponse.ok("A new password is required", new LoginResponse(
                    null, user.getId(), user.getEmail(), user.getDefaultRole(), roles(user),
                    user.getFullName(), true, false
            ));
        }
        return ApiResponse.ok("Login successful", response(user, user.getDefaultRole(), true));
    }

    @PostMapping("/signup/request-code")
    ApiResponse<Void> requestSignupCode(@Valid @RequestBody SignupCodeRequest request) {
        signup.requestCode(request.email());
        return ApiResponse.ok("If the account is eligible, a verification code has been sent", null);
    }

    @PostMapping("/signup/complete")
    ApiResponse<Void> completeSignup(@Valid @RequestBody SignupCompleteRequest request) {
        signup.complete(request.email(), request.code(), request.newPassword());
        return ApiResponse.ok("Account activated. You can now sign in", null);
    }

    @PostMapping("/complete-temporary-password")
    ApiResponse<LoginResponse> completeTemporaryPassword(@Valid @RequestBody CompleteTemporaryPasswordRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() != UserStatus.ACTIVE || !user.isPasswordChangeRequired()
                || user.getPasswordHash() == null || !encoder.matches(request.temporaryPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getTemporaryPasswordExpiresAt() == null
                || !user.getTemporaryPasswordExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("TEMPORARY_PASSWORD_EXPIRED", "Ask an administrator for a new temporary password");
        }
        if (request.temporaryPassword().equals(request.newPassword())) {
            throw new BusinessException("PASSWORD_UNCHANGED", "Choose a password different from the temporary password");
        }
        user.setPasswordHash(encoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordExpiresAt(null);
        users.save(user);
        return ApiResponse.ok("Password changed", response(user, user.getDefaultRole(), true));
    }

    @GetMapping("/sso/config")
    ApiResponse<SsoLoginService.SsoConfiguration> ssoConfiguration() {
        return ApiResponse.ok("SQU SSO configuration", sso.configuration());
    }

    @PostMapping("/sso/exchange")
    ApiResponse<LoginResponse> exchangeSsoCode(@Valid @RequestBody SsoExchangeRequest request) {
        User user = sso.exchange(request.code());
        return ApiResponse.ok("SQU login successful", response(user, user.getDefaultRole(), true));
    }

    @PostMapping("/industry/activate")
    ApiResponse<LoginResponse> activateIndustryGuest(@Valid @RequestBody IndustryActivationRequest request) {
        User user = industryInvitations.activate(request.token(), request.newPassword());
        return ApiResponse.ok("Industry Guest account activated", response(user, user.getDefaultRole(), true));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        currentUsers.requireUser(authorization);
        return ApiResponse.ok("Logout successful", null);
    }

    @PostMapping("/refresh-token")
    ApiResponse<LoginResponse> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        User user = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Token refreshed", response(user, user.getRole(), false));
    }

    @PostMapping("/switch-role")
    ApiResponse<LoginResponse> switchRole(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody SwitchRoleRequest request
    ) {
        User user = currentUsers.requireUser(authorization);
        currentUsers.requireRoleAvailable(user, request.role());
        return ApiResponse.ok("Workspace changed", response(user, request.role(), false));
    }

    @GetMapping("/me")
    ApiResponse<AuthUserResponse> me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        User user = currentUsers.requireUser(authorization);
        return ApiResponse.ok("Current user", new AuthUserResponse(
                user.getId(), user.getUniversityId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), roles(user),
                user.getAccessExpiresAt()
        ));
    }

    @GetMapping("/validate-token")
    ApiResponse<TokenValidationResponse> validateToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        User user = currentUsers.requireUser(authorization);
        TokenService.TokenClaims claims = tokens.parse(bearer(authorization));
        return ApiResponse.ok("Token is valid", new TokenValidationResponse(
                true, user.getId(), user.getRole(), roles(user), claims.expiresAt()
        ));
    }

    @PostMapping("/change-password")
    ApiResponse<Void> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = currentUsers.requireUser(authorization);
        if (!user.isIndustryOnly() && !localInternalLoginEnabled) {
            throw new BusinessException("PASSWORD_MANAGED_BY_SSO", "The password for this account is managed by SQU SSO");
        }
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("PASSWORD_UNCHANGED", "New password must be different");
        }
        user.setPasswordHash(encoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordExpiresAt(null);
        users.save(user);
        return ApiResponse.ok("Password changed", null);
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResets.request(request.email());
        return ApiResponse.ok("If the account exists, a reset email has been sent", null);
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResets.reset(request.token(), request.newPassword());
        return ApiResponse.ok("Password reset successful", null);
    }

    private LoginResponse response(User user, UserRole activeRole, boolean requireSelection) {
        currentUsers.requireRoleAvailable(user, activeRole);
        Set<UserRole> availableRoles = roles(user);
        return new LoginResponse(
                tokens.generate(user, activeRole), user.getId(), user.getEmail(), activeRole, availableRoles,
                user.getFullName(), false, requireSelection && availableRoles.size() > 1
        );
    }

    private Set<UserRole> roles(User user) {
        return Arrays.stream(UserRole.values())
                .filter(user::hasRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
        Set<UserRole> roles,
        LocalDateTime accessExpiresAt
) {}
record TokenValidationResponse(boolean valid, UUID userId, UserRole role, Set<UserRole> roles, long expiresAt) {}
record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
record ForgotPasswordRequest(@NotBlank @Email String email) {}
record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 128) String newPassword) {}
record SignupCodeRequest(@NotBlank @Email String email) {}
record SignupCompleteRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 6) String code,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
record CompleteTemporaryPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String temporaryPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
record SsoExchangeRequest(@NotBlank String code) {}
record IndustryActivationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
record SwitchRoleRequest(@NotNull UserRole role) {}
