package fyp_grading_platform.user;

import fyp_grading_platform.auth.SecureCredentialGenerator;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.notification.EmailDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SecureCredentialGenerator credentials;
    @Mock EmailDeliveryService emails;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users, passwordEncoder, credentials, emails, true, 24);
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) user.setId(UUID.randomUUID());
            return user;
        });
    }

    @Test
    void defaultCreationPreRegistersAccountWithoutPassword() {
        User created = service.create(request(false));

        assertEquals(UserStatus.PENDING_ACTIVATION, created.getStatus());
        assertNull(created.getPasswordHash());
        assertFalse(created.isPasswordChangeRequired());
        verify(emails).send(
                eq(created.getEmail()),
                eq("Your FYP platform account is ready for sign-up"),
                contains("select Sign up"),
                eq(null)
        );
    }

    @Test
    void immediateCreationEmailsTemporaryPasswordAndForcesReplacement() {
        when(credentials.temporaryPassword()).thenReturn("TempPass@483921");
        when(passwordEncoder.encode("TempPass@483921")).thenReturn("encoded-temporary-password");

        User created = service.create(request(true));

        assertEquals(UserStatus.ACTIVE, created.getStatus());
        assertEquals("encoded-temporary-password", created.getPasswordHash());
        assertTrue(created.isPasswordChangeRequired());
        assertNotNull(created.getTemporaryPasswordExpiresAt());
        verify(emails).send(
                eq(created.getEmail()),
                eq("Your temporary FYP platform password"),
                contains("TempPass@483921"),
                eq(null)
        );
    }

    private UserRequest request(boolean activateImmediately) {
        return new UserRequest(
                "STAFF-100",
                "Example Faculty User",
                "faculty.example@squ.edu.om",
                null,
                UserRole.FACULTY_EVALUATOR,
                null,
                activateImmediately
        );
    }
}
