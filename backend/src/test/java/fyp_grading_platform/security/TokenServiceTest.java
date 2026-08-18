package fyp_grading_platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {
    private static final String SECRET = "unit-test-secret-with-more-than-thirty-two-characters";

    @Test
    void signsAndReadsAccountClaims() {
        TokenService service = new TokenService(new ObjectMapper(), SECRET, 300);
        User user = user();

        TokenService.TokenClaims claims = service.parse(service.generate(user));

        assertEquals(user.getId(), claims.subject());
        assertEquals(user.getEmail(), claims.email());
        assertEquals(UserRole.FACULTY_EVALUATOR, claims.role());
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        User user = user();
        TokenService service = new TokenService(new ObjectMapper(), SECRET, 300);
        String token = service.generate(user);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThrows(BusinessException.class, () -> service.parse(tampered));
        TokenService expired = new TokenService(new ObjectMapper(), SECRET, -1);
        assertThrows(BusinessException.class, () -> expired.parse(expired.generate(user)));
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("evaluator@squ.edu.om");
        user.setRole(UserRole.FACULTY_EVALUATOR);
        return user;
    }
}