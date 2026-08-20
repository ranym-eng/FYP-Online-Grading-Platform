package fyp_grading_platform.user;

import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.auth.OneTimeTokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenHasher tokenHasher;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, OneTimeTokenHasher tokenHasher) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
    }

    public User create(UserRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email already exists");
        }
        if (users.existsByUniversityId(request.universityId())) {
            throw new BusinessException("DUPLICATE_UNIVERSITY_ID", "University ID already exists");
        }
        User user = new User();
        apply(user, request);
        String initialPassword = request.password() == null || request.password().isBlank()
                ? tokenHasher.generate()
                : request.password();
        user.setPasswordHash(passwordEncoder.encode(initialPassword));
        return users.save(user);
    }

    public User update(UUID id, UserRequest request) {
        User user = users.findById(id).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        apply(user, request);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return users.save(user);
    }

    public User setStatus(UUID id, UserStatus status) {
        User user = users.findById(id).orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));
        user.setStatus(status);
        return users.save(user);
    }

    private void apply(User user, UserRequest request) {
        user.setUniversityId(request.universityId());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setAccessExpiresAt(request.role() == UserRole.INDUSTRY_REPRESENTATIVE
                ? request.accessExpiresAt()
                : null);
        if (request.role() == UserRole.INDUSTRY_REPRESENTATIVE && request.accessExpiresAt() == null) {
            throw new BusinessException("ACCESS_EXPIRY_REQUIRED", "Industry Guest access requires an expiration date");
        }
    }
}
