package fyp_grading_platform.security;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.time.LocalDateTime;

@Service
public class CurrentUserService {
    private final UserRepository users;
    private final TokenService tokens;
    private final boolean allowEmailToken;

    public CurrentUserService(
            UserRepository users,
            TokenService tokens,
            @Value("${app.security.allow-email-token:false}") boolean allowEmailToken
    ) {
        this.users = users;
        this.tokens = tokens;
        this.allowEmailToken = allowEmailToken;
    }

    public User requireUser(String authorization) {
        String token = bearerToken(authorization);
        if (allowEmailToken && !token.contains(".")) {
            return requireActive(users.findByEmailIgnoreCase(token)
                    .orElseThrow(() -> authenticationRequired()));
        }

        TokenService.TokenClaims claims = tokens.parse(token);
        User user = users.findById(claims.subject())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Authenticated user was not found"));
        requireActive(user);
        if (!user.getEmail().equalsIgnoreCase(claims.email()) || user.getRole() != claims.role()) {
            throw new BusinessException("STALE_TOKEN", "Account permissions changed; sign in again");
        }
        return user;
    }

    public User requireAdmin(String authorization) {
        return requireAnyRole(authorization, UserRole.ADMIN);
    }

    public User requireAnyRole(String authorization, UserRole... roles) {
        User user = requireUser(authorization);
        if (Arrays.stream(roles).noneMatch(role -> role == user.getRole())) {
            throw new BusinessException("ACCESS_DENIED", "This account is not allowed to perform this action");
        }
        return user;
    }

    private User requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Account is inactive");
        }
        if (user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE
                && (user.getAccessExpiresAt() == null || user.getAccessExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new BusinessException("ACCESS_EXPIRED", "Industry Guest access has expired");
        }
        return user;
    }

    private String bearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) throw authenticationRequired();
        String value = authorization.trim();
        if (!value.regionMatches(true, 0, "Bearer ", 0, 7)) throw authenticationRequired();
        String token = value.substring(7).trim();
        if (token.isBlank()) throw authenticationRequired();
        return token;
    }

    private BusinessException authenticationRequired() {
        return new BusinessException("AUTHENTICATION_REQUIRED", "A valid authentication token is required");
    }
}
