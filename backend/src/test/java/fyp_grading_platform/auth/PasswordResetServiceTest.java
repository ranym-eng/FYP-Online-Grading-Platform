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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock UserRepository users;
    @Mock PasswordResetTokenRepository resetTokens;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OneTimeTokenHasher tokenHasher;
    @Mock EmailDeliveryService emails;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                users, resetTokens, passwordEncoder, tokenHasher, emails,
                false, 30, "http://localhost:3010"
        );
    }

    @Test
    void requestSendsOneTimeLinkForActiveIndustryGuest() {
        User guest = industryGuest();
        when(users.findByEmailIgnoreCase(guest.getEmail())).thenReturn(Optional.of(guest));
        when(tokenHasher.generate()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        service.request(guest.getEmail());

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokens).deleteByUserId(guest.getId());
        verify(resetTokens).save(saved.capture());
        assertEquals("hashed-token", saved.getValue().getTokenHash());
        assertNotNull(saved.getValue().getExpiresAt());
        verify(emails).send(
                eq(guest.getEmail()),
                eq("Reset your FYP platform password"),
                org.mockito.ArgumentMatchers.contains("resetToken=raw-token"),
                eq(null)
        );
    }

    @Test
    void unknownAddressKeepsGenericFlowWithoutSendingEmail() {
        when(users.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        service.request("unknown@example.com");

        verify(emails, never()).send(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void validLinkChangesPasswordAndBecomesUsed() {
        User guest = industryGuest();
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(guest);
        reset.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(resetTokens.findByTokenHashAndUsedAtIsNull("hashed-token")).thenReturn(Optional.of(reset));
        when(passwordEncoder.encode("NewPassword@2027")).thenReturn("encoded-password");

        service.reset("raw-token", "NewPassword@2027");

        assertEquals("encoded-password", guest.getPasswordHash());
        assertNotNull(reset.getUsedAt());
        verify(users).save(guest);
        verify(resetTokens).save(reset);
    }

    private User industryGuest() {
        User guest = new User();
        guest.setId(UUID.randomUUID());
        guest.setEmail("industry.guest@example.com");
        guest.setRole(UserRole.INDUSTRY_REPRESENTATIVE);
        guest.setStatus(UserStatus.ACTIVE);
        guest.setAccessExpiresAt(LocalDateTime.now().plusMonths(2));
        return guest;
    }
}
