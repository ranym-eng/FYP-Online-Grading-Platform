package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenHasher tokenHasher;
    private final EmailDeliveryService emails;
    private final boolean localInternalLoginEnabled;
    private final int resetMinutes;
    private final String frontendUrl;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            PasswordEncoder passwordEncoder,
            OneTimeTokenHasher tokenHasher,
            EmailDeliveryService emails,
            @Value("${app.auth.local-internal-login-enabled:false}") boolean localInternalLoginEnabled,
            @Value("${app.auth.password-reset-minutes:30}") int resetMinutes,
            @Value("${app.frontend-url:http://localhost:3010}") String frontendUrl
    ) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.emails = emails;
        this.localInternalLoginEnabled = localInternalLoginEnabled;
        this.resetMinutes = resetMinutes;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void request(String email) {
        users.findByEmailIgnoreCase(email.trim())
                .filter(this::canResetPassword)
                .ifPresent(this::sendResetLink);
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken reset = resetTokens
                .findByTokenHashAndUsedAtIsNull(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new BusinessException(
                        "INVALID_RESET_TOKEN",
                        "Password reset link is invalid or has already been used"
                ));
        LocalDateTime now = LocalDateTime.now();
        if (!reset.getExpiresAt().isAfter(now)) {
            throw new BusinessException("RESET_TOKEN_EXPIRED", "Password reset link has expired");
        }
        User user = reset.getUser();
        if (!canResetPassword(user)) {
            throw new BusinessException("PASSWORD_RESET_NOT_ALLOWED", "Password reset is not available for this account");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordExpiresAt(null);
        users.save(user);
        reset.setUsedAt(now);
        resetTokens.save(reset);
    }

    private boolean canResetPassword(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) return false;
        if (user.getRole() != UserRole.INDUSTRY_REPRESENTATIVE) return localInternalLoginEnabled;
        return user.getAccessExpiresAt() != null && user.getAccessExpiresAt().isAfter(LocalDateTime.now());
    }

    private void sendResetLink(User user) {
        resetTokens.deleteByUserId(user.getId());
        String rawToken = tokenHasher.generate();
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(user);
        reset.setTokenHash(tokenHasher.hash(rawToken));
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(resetMinutes));
        resetTokens.save(reset);

        String link = appendResetToken(rawToken);
        emails.send(
                user.getEmail(),
                "Reset your FYP platform password",
                "A password reset was requested for your FYP platform account.\n\n"
                        + "Choose a new password within " + resetMinutes + " minutes:\n" + link + "\n\n"
                        + "This link is valid once. If you did not request it, you can ignore this message.",
                null
        );
    }

    private String appendResetToken(String rawToken) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + "resetToken="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
