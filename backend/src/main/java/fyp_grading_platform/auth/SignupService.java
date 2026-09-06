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

import java.time.LocalDateTime;

@Service
public class SignupService {
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository users;
    private final SignupVerificationCodeRepository codes;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenHasher tokenHasher;
    private final SecureCredentialGenerator credentials;
    private final EmailDeliveryService emails;
    private final boolean localInternalLoginEnabled;
    private final int codeMinutes;

    public SignupService(
            UserRepository users,
            SignupVerificationCodeRepository codes,
            PasswordEncoder passwordEncoder,
            OneTimeTokenHasher tokenHasher,
            SecureCredentialGenerator credentials,
            EmailDeliveryService emails,
            @Value("${app.auth.local-internal-login-enabled:false}") boolean localInternalLoginEnabled,
            @Value("${app.auth.signup-code-minutes:10}") int codeMinutes
    ) {
        this.users = users;
        this.codes = codes;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.credentials = credentials;
        this.emails = emails;
        this.localInternalLoginEnabled = localInternalLoginEnabled;
        this.codeMinutes = codeMinutes;
    }

    @Transactional
    public void requestCode(String email) {
        users.findByEmailIgnoreCase(normalize(email))
                .filter(this::canSignUp)
                .ifPresent(this::sendCode);
    }

    @Transactional
    public User complete(String email, String code, String newPassword) {
        User user = users.findByEmailIgnoreCase(normalize(email))
                .filter(this::canSignUp)
                .orElseThrow(() -> invalidCode());
        SignupVerificationCode verification = codes
                .findTopByUserIdAndVerifiedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> invalidCode());
        LocalDateTime now = LocalDateTime.now();
        if (!verification.getExpiresAt().isAfter(now)) {
            throw new BusinessException("SIGNUP_CODE_EXPIRED", "The verification code has expired");
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new BusinessException("SIGNUP_CODE_LOCKED", "Request a new verification code");
        }
        String submittedHash = hash(user, code.trim());
        if (!verification.getCodeHash().equals(submittedHash)) {
            verification.setFailedAttempts(verification.getFailedAttempts() + 1);
            codes.save(verification);
            throw invalidCode();
        }
        ensureGuestAccessIsCurrent(user, now);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setTemporaryPasswordExpiresAt(null);
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);
        verification.setVerifiedAt(now);
        codes.save(verification);
        return user;
    }

    private void sendCode(User user) {
        SignupVerificationCode recent = codes
                .findTopByUserIdAndVerifiedAtIsNullOrderByCreatedAtDesc(user.getId())
                .orElse(null);
        if (recent != null && recent.getCreatedAt() != null
                && recent.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            return;
        }
        codes.deleteByUserId(user.getId());
        String rawCode = credentials.verificationCode();
        SignupVerificationCode verification = new SignupVerificationCode();
        verification.setUser(user);
        verification.setCodeHash(hash(user, rawCode));
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(codeMinutes));
        codes.save(verification);
        emails.send(
                user.getEmail(),
                "Your FYP platform verification code",
                "Use this code to complete your FYP platform sign-up:\n\n"
                        + rawCode + "\n\nThe code expires in " + codeMinutes
                        + " minutes and can be used once. If you did not request it, ignore this message.",
                null
        );
    }

    private boolean canSignUp(User user) {
        if (user.getStatus() != UserStatus.PENDING_ACTIVATION
                && user.getStatus() != UserStatus.PENDING_INVITATION) {
            return false;
        }
        if (user.getRole() != UserRole.INDUSTRY_REPRESENTATIVE && !localInternalLoginEnabled) {
            return false;
        }
        return user.getRole() != UserRole.INDUSTRY_REPRESENTATIVE
                || user.getAccessExpiresAt() != null && user.getAccessExpiresAt().isAfter(LocalDateTime.now());
    }

    private void ensureGuestAccessIsCurrent(User user, LocalDateTime now) {
        if (user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE
                && (user.getAccessExpiresAt() == null || !user.getAccessExpiresAt().isAfter(now))) {
            user.setStatus(UserStatus.INACTIVE);
            users.save(user);
            throw new BusinessException("ACCESS_EXPIRED", "Industry Guest access has expired");
        }
    }

    private String hash(User user, String code) {
        return tokenHasher.hash(user.getId() + ":" + code);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private BusinessException invalidCode() {
        return new BusinessException("INVALID_SIGNUP_CODE", "The email address or verification code is invalid");
    }
}
