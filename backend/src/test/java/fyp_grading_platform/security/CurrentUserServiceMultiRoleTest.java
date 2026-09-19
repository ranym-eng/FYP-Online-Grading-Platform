package fyp_grading_platform.security;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceMultiRoleTest {
    @Mock UserRepository users;
    @Mock TokenService tokens;

    private CurrentUserService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CurrentUserService(users, tokens, false);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("multi.role@squ.edu.om");
        user.setRoles(Set.of(UserRole.ADMIN, UserRole.SUPERVISOR));
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void restoresTheSignedActiveRole() {
        when(tokens.parse("signed-token")).thenReturn(new TokenService.TokenClaims(
                user.getId(), user.getEmail(), UserRole.SUPERVISOR, Long.MAX_VALUE
        ));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        User authenticated = service.requireUser("Bearer signed-token");

        assertEquals(UserRole.SUPERVISOR, authenticated.getRole());
        assertEquals(UserRole.ADMIN, authenticated.getDefaultRole());
    }

    @Test
    void rejectsATokenForARoleThatIsNoLongerAssigned() {
        when(tokens.parse("signed-token")).thenReturn(new TokenService.TokenClaims(
                user.getId(), user.getEmail(), UserRole.COORDINATOR, Long.MAX_VALUE
        ));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> service.requireUser("Bearer signed-token"));
    }
}
