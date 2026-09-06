package fyp_grading_platform.config;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;

    private final DataInitializer initializer = new DataInitializer();

    @Test
    void createsConfiguredPrincipalAdministrator() {
        when(encoder.encode("private-password")).thenReturn("encoded-password");

        initializer.seedBootstrapAdmin(
                users,
                encoder,
                true,
                " Principal.Admin@SQU.edu.om ",
                " Principal Administrator ",
                " ADMIN-PRINCIPAL ",
                "private-password"
        );

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User admin = saved.getValue();
        assertEquals("principal.admin@squ.edu.om", admin.getEmail());
        assertEquals("Principal Administrator", admin.getFullName());
        assertEquals("ADMIN-PRINCIPAL", admin.getUniversityId());
        assertEquals("encoded-password", admin.getPasswordHash());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        assertFalse(admin.isPasswordChangeRequired());
    }

    @Test
    void leavesAnExistingAccountAndPasswordUntouched() {
        User existing = new User();
        existing.setEmail("principal.admin@squ.edu.om");
        existing.setFullName("Principal Administrator");
        existing.setUniversityId("ADMIN-PRINCIPAL");
        existing.setPasswordHash("existing-password-hash");
        existing.setRole(UserRole.ADMIN);
        existing.setStatus(UserStatus.ACTIVE);
        when(users.findByEmailIgnoreCase("principal.admin@squ.edu.om")).thenReturn(Optional.of(existing));

        initializer.seedBootstrapAdmin(
                users,
                encoder,
                true,
                "principal.admin@squ.edu.om",
                "Principal Administrator",
                "ADMIN-PRINCIPAL",
                "private-password"
        );

        verify(encoder, never()).encode("private-password");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void activatesAndPromotesAPreExistingProvisionedAccountWithoutAPassword() {
        User existing = new User();
        existing.setEmail("principal.admin@squ.edu.om");
        existing.setFullName("Imported User");
        existing.setUniversityId("IMPORTED-001");
        existing.setRole(UserRole.COORDINATOR);
        existing.setStatus(UserStatus.PENDING_INVITATION);
        when(users.findByEmailIgnoreCase("principal.admin@squ.edu.om")).thenReturn(Optional.of(existing));
        when(encoder.encode("private-password")).thenReturn("encoded-password");

        initializer.seedBootstrapAdmin(
                users,
                encoder,
                true,
                "principal.admin@squ.edu.om",
                "Principal Administrator",
                "ADMIN-PRINCIPAL",
                "private-password"
        );

        assertEquals("Principal Administrator", existing.getFullName());
        assertEquals("ADMIN-PRINCIPAL", existing.getUniversityId());
        assertEquals("encoded-password", existing.getPasswordHash());
        assertEquals(UserRole.ADMIN, existing.getRole());
        assertEquals(UserStatus.ACTIVE, existing.getStatus());
        verify(users).save(existing);
    }

    @Test
    void doesNothingWhenBootstrapIsDisabled() {
        initializer.seedBootstrapAdmin(users, encoder, false, "", "", "", "");

        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAUniversityIdOwnedByAnotherAccount() {
        when(users.existsByUniversityId("ADMIN-PRINCIPAL")).thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> initializer.seedBootstrapAdmin(
                        users,
                        encoder,
                        true,
                        "principal.admin@squ.edu.om",
                        "Principal Administrator",
                        "ADMIN-PRINCIPAL",
                        "private-password"
                )
        );

        assertTrue(error.getMessage().contains("ADMIN-PRINCIPAL"));
        verify(users, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
