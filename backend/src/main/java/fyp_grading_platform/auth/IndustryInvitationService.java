package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.notification.EmailDeliveryService;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class IndustryInvitationService {
    private final IndustryInvitationRepository invitations;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenHasher tokenHasher;
    private final EmailDeliveryService emails;
    private final int invitationHours;
    private final String frontendUrl;

    public IndustryInvitationService(
            IndustryInvitationRepository invitations,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            OneTimeTokenHasher tokenHasher,
            EmailDeliveryService emails,
            @Value("${app.auth.industry-invitation-hours:48}") int invitationHours,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.invitations = invitations;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.emails = emails;
        this.invitationHours = invitationHours;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void invite(User user) {
        if (user.getRole() != UserRole.INDUSTRY_REPRESENTATIVE) {
            throw new BusinessException("INDUSTRY_ACCOUNT_REQUIRED", "Only Industry Guests receive external invitations");
        }
        if (user.getAccessExpiresAt() == null) {
            throw new BusinessException("ACCESS_EXPIRY_REQUIRED", "Industry Guest access requires an expiration date");
        }
        if (user.getAccessExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("ACCESS_ALREADY_EXPIRED", "Industry Guest access has already expired");
        }

        invitations.deleteByUserId(user.getId());
        String rawToken = tokenHasher.generate();
        IndustryInvitation invitation = new IndustryInvitation();
        invitation.setUser(user);
        invitation.setTokenHash(tokenHasher.hash(rawToken));
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationHours));
        invitations.save(invitation);

        user.setStatus(UserStatus.PENDING_INVITATION);
        users.save(user);
        String link = appendInvitation(rawToken);
        emails.send(
                user.getEmail(),
                "Invitation to the SQU FYP grading platform",
                "You have been invited as an Industry Guest.\n\n"
                        + "Activate your account within " + invitationHours + " hours:\n" + link + "\n\n"
                        + "Your access is limited to assigned Demo Day projects and expires on "
                        + user.getAccessExpiresAt() + ".",
                null
        );
    }

    @Transactional
    public User activate(String rawToken, String newPassword) {
        IndustryInvitation invitation = invitations
                .findByTokenHashAndAcceptedAtIsNull(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new BusinessException("INVALID_INVITATION", "The Industry Guest invitation is invalid"));
        LocalDateTime now = LocalDateTime.now();
        if (invitation.getExpiresAt().isBefore(now)) {
            throw new BusinessException("INVITATION_EXPIRED", "The Industry Guest invitation has expired");
        }
        User user = invitation.getUser();
        if (user.getAccessExpiresAt() == null || user.getAccessExpiresAt().isBefore(now)) {
            user.setStatus(UserStatus.INACTIVE);
            users.save(user);
            throw new BusinessException("ACCESS_EXPIRED", "Industry Guest access has expired");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);
        invitation.setAcceptedAt(now);
        invitations.save(invitation);
        return user;
    }

    private String appendInvitation(String rawToken) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + "industryInvitation="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
