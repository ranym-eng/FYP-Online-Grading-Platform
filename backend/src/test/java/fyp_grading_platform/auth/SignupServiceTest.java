package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {
    @Mock UserRepository users;
    @Mock SignupVerificationCodeRepository codes;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OneTimeTokenHasher tokenHasher;
    @Mock SecureCredentialGenerator credentials;
    @Mock EmailDeliveryService emails;

    private SignupService service;

    @BeforeEach
    void setUp() {
        service = new SignupService(users, codes, passwordEncoder, tokenHasher, credentials, emails, true, 10);
    }

    @Test
    void preRegisteredUserReceivesSixDigitCode() {
        User user = pendingUser();
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(credentials.verificationCode()).thenReturn("483921");
        when(tokenHasher.hash(user.getId() + ":483921")).thenReturn("hashed-code");

        service.requestCode(user.getEmail());

        ArgumentCaptor<SignupVerificationCode> saved = ArgumentCaptor.forClass(SignupVerificationCode.class);
        verify(codes).save(saved.capture());
        assertEquals("hashed-code", saved.getValue().getCodeHash());
        assertEquals(user, saved.getValue().getUser());
        verify(emails).send(
                eq(user.getEmail()),
                eq("Your FYP platform verification code"),
                org.mockito.ArgumentMatchers.contains("483921"),
                eq(null)
        );
    }

    @Test
    void unknownEmailKeepsGenericFlowWithoutSendingMail() {
        when(users.findByEmailIgnoreCase("unknown@squ.edu.om")).thenReturn(Optional.empty());

        service.requestCode("unknown@squ.edu.om");

        verify(emails, never()).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void validCodeActivatesAccountWithChosenPassword() {
        User user = pendingUser();
        SignupVerificationCode verification = new SignupVerificationCode();
        verification.setUser(user);
        verification.setCodeHash("hashed-code");
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(codes.findTopByUserIdAndVerifiedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(verification));
        when(tokenHasher.hash(user.getId() + ":483921")).thenReturn("hashed-code");
        when(passwordEncoder.encode("Personal@2027")).thenReturn("encoded-password");

        User activated = service.complete(user.getEmail(), "483921", "Personal@2027");

        assertEquals(UserStatus.ACTIVE, activated.getStatus());
        assertEquals("encoded-password", activated.getPasswordHash());
        assertFalse(activated.isPasswordChangeRequired());
        assertNull(activated.getTemporaryPasswordExpiresAt());
        verify(users).save(user);
        verify(codes).save(verification);
    }

    private User pendingUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUniversityId("STAFF-100");
        user.setFullName("Example Faculty User");
        user.setEmail("faculty.example@squ.edu.om");
        user.setRole(UserRole.FACULTY_EVALUATOR);
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        return user;
    }
}
