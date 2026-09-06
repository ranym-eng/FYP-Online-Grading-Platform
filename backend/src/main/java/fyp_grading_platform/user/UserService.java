package fyp_grading_platform.user;

import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.auth.SecureCredentialGenerator;
import fyp_grading_platform.notification.EmailDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureCredentialGenerator credentials;
    private final EmailDeliveryService emails;
    private final boolean localInternalLoginEnabled;
    private final int temporaryPasswordHours;

    public UserService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            SecureCredentialGenerator credentials,
            EmailDeliveryService emails,
            @Value("${app.auth.local-internal-login-enabled:false}") boolean localInternalLoginEnabled,
            @Value("${app.auth.temporary-password-hours:24}") int temporaryPasswordHours
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.credentials = credentials;
        this.emails = emails;
        this.localInternalLoginEnabled = localInternalLoginEnabled;
        this.temporaryPasswordHours = temporaryPasswordHours;
    }

    @Transactional
    public User create(UserRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already exists");
        }
        if (users.existsByUniversityId(request.universityId())) {
            throw new BusinessException("DUPLICATE_UNIVERSITY_ID", "University ID already exists");
        }
        User user = new User();
        apply(user, request);
        boolean activateImmediately = Boolean.TRUE.equals(request.activateImmediately());
        if (!activateImmediately) {
            user.setStatus(UserStatus.PENDING_ACTIVATION);
            user.setPasswordHash(null);
            user.setPasswordChangeRequired(false);
            user.setTemporaryPasswordExpiresAt(null);
            user = users.save(user);
            sendSignupInstructions(user);
            return user;
        }
        if (!usesLocalPassword(user)) {
            user.setStatus(UserStatus.ACTIVE);
            user.setPasswordHash(null);
            user.setPasswordChangeRequired(false);
            user.setTemporaryPasswordExpiresAt(null);
            return users.save(user);
        }
        String temporaryPassword = prepareTemporaryPassword(user);
        user = users.save(user);
        sendTemporaryPassword(user, temporaryPassword);
        return user;
    }

    @Transactional
    public User update(UUID id, UserRequest request) {
        User user = users.findById(id).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        users.findByEmailIgnoreCase(request.email())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new BusinessException("DUPLICATE_EMAIL", "Email already exists"); });
        users.findByUniversityId(request.universityId())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new BusinessException("DUPLICATE_UNIVERSITY_ID", "University ID already exists"); });
        apply(user, request);
        return users.save(user);
    }

    @Transactional
    public User issueTemporaryPassword(UUID id) {
        User user = users.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException("ACCOUNT_SUSPENDED", "A suspended account cannot be activated");
        }
        if (user.getStatus() == UserStatus.ACTIVE && !user.isPasswordChangeRequired()) {
            throw new BusinessException("ACCOUNT_ALREADY_ACTIVE", "This account is already active");
        }
        if (!usesLocalPassword(user)) {
            throw new BusinessException("PASSWORD_MANAGED_BY_SSO", "This account must sign in through SQU SSO");
        }
        ensureIndustryAccessIsCurrent(user);
        String temporaryPassword = prepareTemporaryPassword(user);
        user = users.save(user);
        sendTemporaryPassword(user, temporaryPassword);
        return user;
    }

    public User setStatus(UUID id, UserStatus status) {
        User user = users.findById(id).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        user.setStatus(status);
        return users.save(user);
    }

    private void apply(User user, UserRequest request) {
        user.setUniversityId(request.universityId().trim());
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setAccessExpiresAt(request.role() == UserRole.INDUSTRY_REPRESENTATIVE
                ? request.accessExpiresAt()
                : null);
        if (request.role() == UserRole.INDUSTRY_REPRESENTATIVE && request.accessExpiresAt() == null) {
            throw new BusinessException("ACCESS_EXPIRY_REQUIRED", "Industry Guest access requires an expiration date");
        }
    }

    private String prepareTemporaryPassword(User user) {
        String temporaryPassword = credentials.temporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordChangeRequired(true);
        user.setTemporaryPasswordExpiresAt(LocalDateTime.now().plusHours(temporaryPasswordHours));
        user.setStatus(UserStatus.ACTIVE);
        return temporaryPassword;
    }

    private void sendSignupInstructions(User user) {
        emails.send(
                user.getEmail(),
                "Your FYP platform account is ready for sign-up",
                "The FYP administration has pre-registered your account.\n\n"
                        + "Open the platform, select Sign up, and use this email address to create your password.\n\n"
                        + "Your role and project access have already been assigned by the administration.",
                null
        );
    }

    private void sendTemporaryPassword(User user, String temporaryPassword) {
        emails.send(
                user.getEmail(),
                "Your temporary FYP platform password",
                "Your FYP platform account is active.\n\nTemporary password: " + temporaryPassword
                        + "\n\nSign in within " + temporaryPasswordHours
                        + " hours. You must choose a new password before entering your workspace.",
                null
        );
    }

    private boolean usesLocalPassword(User user) {
        return localInternalLoginEnabled || user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE;
    }

    private void ensureIndustryAccessIsCurrent(User user) {
        if (user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE
                && (user.getAccessExpiresAt() == null || !user.getAccessExpiresAt().isAfter(LocalDateTime.now()))) {
            throw new BusinessException("ACCESS_EXPIRED", "Industry Guest access has expired");
        }
    }
}
