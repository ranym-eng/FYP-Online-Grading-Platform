package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.api.ApiResponse;
import fyp_grading_platform.security.CurrentUserService;
import fyp_grading_platform.security.TokenService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerPasswordChangeTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock TokenService tokens;
    @Mock CurrentUserService currentUsers;
    @Mock PasswordResetService passwordResets;
    @Mock SignupService signup;
    @Mock SsoLoginService sso;
    @Mock IndustryInvitationService industryInvitations;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                users, encoder, tokens, currentUsers, passwordResets, signup, sso, industryInvitations, true
        );
    }

    @Test
    void temporaryPasswordLoginCannotOpenWorkspace() {
        User user = temporaryPasswordUser();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("Temporary@42", "temporary-hash")).thenReturn(true);

        ApiResponse<LoginResponse> result = controller.login(new LoginRequest(user.getEmail(), "Temporary@42"));

        assertTrue(result.data().passwordChangeRequired());
        assertNull(result.data().token());
    }

    @Test
    void replacingTemporaryPasswordReturnsUsableSession() {
        User user = temporaryPasswordUser();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("Temporary@42", "temporary-hash")).thenReturn(true);
        when(encoder.encode("Personal@2027")).thenReturn("personal-hash");
        when(tokens.generate(user)).thenReturn("session-token");

        ApiResponse<LoginResponse> result = controller.completeTemporaryPassword(
                new CompleteTemporaryPasswordRequest(user.getEmail(), "Temporary@42", "Personal@2027")
        );

        assertFalse(result.data().passwordChangeRequired());
        assertFalse(user.isPasswordChangeRequired());
        assertNull(user.getTemporaryPasswordExpiresAt());
        verify(users).save(user);
    }

    private User temporaryPasswordUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUniversityId("STAFF-100");
        user.setFullName("Example Faculty User");
        user.setEmail("faculty.example@squ.edu.om");
        user.setRole(UserRole.FACULTY_EVALUATOR);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("temporary-hash");
        user.setPasswordChangeRequired(true);
        user.setTemporaryPasswordExpiresAt(LocalDateTime.now().plusHours(2));
        return user;
    }
}
