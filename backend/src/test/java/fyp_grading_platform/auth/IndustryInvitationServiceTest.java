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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustryInvitationServiceTest {
    @Mock IndustryInvitationRepository invitations;
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OneTimeTokenHasher tokenHasher;
    @Mock EmailDeliveryService emails;

    private IndustryInvitationService service;

    @BeforeEach
    void setUp() {
        service = new IndustryInvitationService(
                invitations, users, passwordEncoder, tokenHasher, emails,
                48, "http://localhost:3000"
        );
    }

    @Test
    void invitationCreatesOneTimeLinkAndPutsGuestInPendingState() {
        User guest = industryGuest();
        when(tokenHasher.generate()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        service.invite(guest);

        ArgumentCaptor<IndustryInvitation> invitation = ArgumentCaptor.forClass(IndustryInvitation.class);
        verify(invitations).save(invitation.capture());
        assertEquals("hashed-token", invitation.getValue().getTokenHash());
        assertEquals(guest, invitation.getValue().getUser());
        assertEquals(UserStatus.PENDING_INVITATION, guest.getStatus());
        verify(emails).send(
                eq(guest.getEmail()),
                eq("Invitation to the SQU FYP grading platform"),
                org.mockito.ArgumentMatchers.contains("industryInvitation=raw-token"),
                eq(null)
        );
    }

    @Test
    void validInvitationActivatesGuestAndCanOnlyBeAcceptedOnce() {
        User guest = industryGuest();
        guest.setStatus(UserStatus.PENDING_INVITATION);
        IndustryInvitation invitation = new IndustryInvitation();
        invitation.setUser(guest);
        invitation.setTokenHash("hashed-token");
        invitation.setExpiresAt(LocalDateTime.now().plusHours(2));
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(invitations.findByTokenHashAndAcceptedAtIsNull("hashed-token")).thenReturn(Optional.of(invitation));
        when(passwordEncoder.encode("Guest@2027")).thenReturn("encoded-password");

        User activated = service.activate("raw-token", "Guest@2027");

        assertEquals(UserStatus.ACTIVE, activated.getStatus());
        assertEquals("encoded-password", activated.getPasswordHash());
        assertNotNull(invitation.getAcceptedAt());
        verify(users).save(guest);
        verify(invitations).save(invitation);
    }

    private User industryGuest() {
        User guest = new User();
        guest.setId(UUID.randomUUID());
        guest.setUniversityId("DEMO-IND-01");
        guest.setFullName("Example Industry Guest");
        guest.setEmail("demo.industry@example.com");
        guest.setRole(UserRole.INDUSTRY_REPRESENTATIVE);
        guest.setStatus(UserStatus.ACTIVE);
        guest.setAccessExpiresAt(LocalDateTime.now().plusMonths(6));
        return guest;
    }
}
