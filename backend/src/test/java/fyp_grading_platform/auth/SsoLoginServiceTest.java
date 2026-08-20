package fyp_grading_platform.auth;

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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SsoLoginServiceTest {
    @Mock UserRepository users;
    @Mock SsoLoginCodeRepository codes;
    @Mock OneTimeTokenHasher tokenHasher;
    @Mock ObjectProvider<ClientRegistrationRepository> registrations;
    @Mock OAuth2User principal;

    private SsoLoginService service;

    @BeforeEach
    void setUp() {
        service = new SsoLoginService(
                users, codes, tokenHasher, registrations,
                true, "squ", "email", "squ.edu.om", "http://localhost:3000", false
        );
    }

    @Test
    void importedInternalIdentityReceivesItsProvisionedRole() {
        User coordinator = user(UserRole.COORDINATOR);
        when(principal.getAttribute("email")).thenReturn("COORDINATOR@SQU.EDU.OM");
        when(users.findByEmailIgnoreCase("coordinator@squ.edu.om")).thenReturn(Optional.of(coordinator));

        User authenticated = service.requireImportedInternalUser(principal);

        assertEquals(coordinator, authenticated);
        assertEquals(UserRole.COORDINATOR, authenticated.getRole());
    }

    @Test
    void unknownSsoIdentityCannotCreateItsOwnAccount() {
        when(principal.getAttribute("email")).thenReturn("unknown@squ.edu.om");
        when(users.findByEmailIgnoreCase("unknown@squ.edu.om")).thenReturn(Optional.empty());

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.requireImportedInternalUser(principal)
        );

        assertEquals("SSO_ACCOUNT_NOT_PROVISIONED", error.getErrorCode());
    }

    private User user(UserRole role) {
        User user = new User();
        user.setEmail("coordinator@squ.edu.om");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
