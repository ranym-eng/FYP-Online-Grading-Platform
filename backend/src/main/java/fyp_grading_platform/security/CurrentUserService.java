package fyp_grading_platform.security;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User requireUser(String authorization) {
        String token = bearerToken(authorization);
        User user = resolveGeneratedToken(token);
        if (user != null) return user;

        // Keeps the existing Swagger development convention: Bearer admin@squ.edu.om.
        return users.findByEmailIgnoreCase(token)
                .orElseThrow(() -> new BusinessException("AUTHENTICATION_REQUIRED", "A valid authentication token is required"));
    }

    public User requireAdmin(String authorization) {
        User user = requireUser(authorization);
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException("ADMIN_REQUIRED", "Only an administrator can perform this action");
        }
        return user;
    }

    private User resolveGeneratedToken(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 4);
            if (parts.length != 4) return null;
            long expiresAt = Long.parseLong(parts[3]);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new BusinessException("TOKEN_EXPIRED", "Authentication token has expired");
            }
            return users.findById(UUID.fromString(parts[0]))
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Authenticated user was not found"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String bearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new BusinessException("AUTHENTICATION_REQUIRED", "Authentication is required");
        }
        String value = authorization.trim();
        return value.regionMatches(true, 0, "Bearer ", 0, 7) ? value.substring(7).trim() : value;
    }
}
