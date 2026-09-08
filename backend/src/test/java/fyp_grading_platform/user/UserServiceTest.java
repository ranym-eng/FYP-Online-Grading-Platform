package fyp_grading_platform.user;

import fyp_grading_platform.auth.SecureCredentialGenerator;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.notification.EmailDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
        lenient().when(users.save(any(User.class))).thenAnswer(invocation -> {
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

    @Test
    void changingRoleInvalidatesTheOldSessionAndForcesPasswordReplacement() {
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setUniversityId("STAFF-100");
        existing.setFullName("Example Faculty User");
        existing.setEmail("faculty.example@squ.edu.om");
        existing.setRole(UserRole.SUPERVISOR);
        existing.setStatus(UserStatus.ACTIVE);
        existing.setPasswordHash("existing-password-hash");
        when(users.findById(existing.getId())).thenReturn(Optional.of(existing));

        User updated = service.update(existing.getId(), request(UserRole.FACULTY_EVALUATOR, false));

        assertEquals(UserRole.FACULTY_EVALUATOR, updated.getRole());
        assertEquals("existing-password-hash", updated.getPasswordHash());
        assertTrue(updated.isPasswordChangeRequired());
        assertNotNull(updated.getTemporaryPasswordExpiresAt());
        verify(emails).send(
                eq(updated.getEmail()),
                eq("Your FYP platform role has changed"),
                contains("choose a new password"),
                eq(null)
        );
    }

    @Test
    void importedActiveAccountReceivesTemporaryPassword() {
        User imported = new User();
        imported.setUniversityId("STAFF-200");
        imported.setFullName("Imported Faculty User");
        imported.setEmail("imported.faculty@squ.edu.om");
        imported.setRole(UserRole.REPORT_EVALUATOR);
        imported.setStatus(UserStatus.ACTIVE);
        when(passwordEncoder.encode("Imported@123")).thenReturn("encoded-import-password");

        User provisioned = service.provisionImportedActiveAccount(imported, "Imported@123");

        assertEquals("encoded-import-password", provisioned.getPasswordHash());
        assertTrue(provisioned.isPasswordChangeRequired());
        assertNotNull(provisioned.getTemporaryPasswordExpiresAt());
        verify(emails).send(
                eq(provisioned.getEmail()),
                eq("Your temporary FYP platform password"),
                contains("Imported@123"),
                eq(null)
        );
    }

    @Test
    void deactivatesAccountWithoutDeletingItsAcademicIdentity() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.SUPERVISOR);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("encoded-current-password");
        user.setPasswordChangeRequired(true);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        User result = service.deactivate(user.getId());

        assertEquals(UserStatus.INACTIVE, result.getStatus());
        assertNull(result.getPasswordHash());
        assertFalse(result.isPasswordChangeRequired());
        assertNull(result.getTemporaryPasswordExpiresAt());
    }

    @Test
    void refusesToDeactivateTheLastActiveAdministrator() {
        User administrator = new User();
        administrator.setId(UUID.randomUUID());
        administrator.setRole(UserRole.ADMIN);
        administrator.setStatus(UserStatus.ACTIVE);
        when(users.findById(administrator.getId())).thenReturn(Optional.of(administrator));
        when(users.findByRole(UserRole.ADMIN)).thenReturn(List.of(administrator));

        BusinessException error = assertThrows(BusinessException.class, () -> service.deactivate(administrator.getId()));

        assertEquals("LAST_ADMIN", error.getErrorCode());
    }

    private UserRequest request(boolean activateImmediately) {
        return request(UserRole.FACULTY_EVALUATOR, activateImmediately);
    }

    private UserRequest request(UserRole role, boolean activateImmediately) {
        return new UserRequest(
                "STAFF-100",
                "Example Faculty User",
                "faculty.example@squ.edu.om",
                null,
                role,
                null,
                activateImmediately
        );
    }
}
