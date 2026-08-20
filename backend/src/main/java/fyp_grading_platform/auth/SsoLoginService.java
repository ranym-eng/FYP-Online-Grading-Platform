package fyp_grading_platform.auth;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import fyp_grading_platform.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class SsoLoginService {
    private final UserRepository users;
    private final SsoLoginCodeRepository codes;
    private final OneTimeTokenHasher tokenHasher;
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final boolean enabled;
    private final String registrationId;
    private final String emailClaim;
    private final String allowedDomain;
    private final String frontendUrl;
    private final boolean localInternalLoginEnabled;

    public SsoLoginService(
            UserRepository users,
            SsoLoginCodeRepository codes,
            OneTimeTokenHasher tokenHasher,
            ObjectProvider<ClientRegistrationRepository> registrations,
            @Value("${app.auth.sso.enabled:false}") boolean enabled,
            @Value("${app.auth.sso.registration-id:squ}") String registrationId,
            @Value("${app.auth.sso.email-claim:email}") String emailClaim,
            @Value("${app.auth.sso.allowed-domain:squ.edu.om}") String allowedDomain,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
            @Value("${app.auth.local-internal-login-enabled:false}") boolean localInternalLoginEnabled
    ) {
        this.users = users;
        this.codes = codes;
        this.tokenHasher = tokenHasher;
        this.registrations = registrations;
        this.enabled = enabled;
        this.registrationId = registrationId;
        this.emailClaim = emailClaim;
        this.allowedDomain = allowedDomain;
        this.frontendUrl = frontendUrl;
        this.localInternalLoginEnabled = localInternalLoginEnabled;
    }

    public boolean configured() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return enabled && repository != null && repository.findByRegistrationId(registrationId) != null;
    }

    public SsoConfiguration configuration() {
        boolean available = configured();
        return new SsoConfiguration(
                available,
                available ? "/oauth2/authorization/" + registrationId : null,
                localInternalLoginEnabled
        );
    }

    public User requireImportedInternalUser(OAuth2User principal) {
        String email = firstNonBlank(
                principal.getAttribute(emailClaim),
                principal.getAttribute("email"),
                principal.getAttribute("preferred_username"),
                principal.getAttribute("upn")
        );
        if (email == null || email.isBlank()) {
            throw new BusinessException("SSO_EMAIL_MISSING", "The SQU identity did not provide an email address");
        }
        String normalized = email.trim().toLowerCase();
        if (!allowedDomain.isBlank() && !normalized.endsWith("@" + allowedDomain.toLowerCase())) {
            throw new BusinessException("SSO_DOMAIN_NOT_ALLOWED", "Only SQU institutional accounts are allowed");
        }
        User user = users.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new BusinessException(
                        "SSO_ACCOUNT_NOT_PROVISIONED",
                        "This SQU account must be imported by an administrator before first access"
                ));
        if (user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE) {
            throw new BusinessException("SSO_INTERNAL_ONLY", "Industry Guests must use their invitation");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE", "This imported account is not active");
        }
        return user;
    }

    @Transactional
    public String issueExchangeCode(User user) {
        codes.deleteByUserId(user.getId());
        String raw = tokenHasher.generate();
        SsoLoginCode code = new SsoLoginCode();
        code.setUser(user);
        code.setTokenHash(tokenHasher.hash(raw));
        code.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        codes.save(code);
        return raw;
    }

    @Transactional
    public User exchange(String rawCode) {
        SsoLoginCode code = codes.findByTokenHashAndUsedAtIsNull(tokenHasher.hash(rawCode))
                .orElseThrow(() -> new BusinessException("INVALID_SSO_CODE", "The SSO login code is invalid"));
        if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("SSO_CODE_EXPIRED", "The SSO login code has expired");
        }
        User user = code.getUser();
        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() == UserRole.INDUSTRY_REPRESENTATIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE", "This account cannot use SQU SSO");
        }
        code.setUsedAt(LocalDateTime.now());
        codes.save(code);
        return user;
    }

    public String successRedirect(String rawCode) {
        return appendParameter("ssoCode", rawCode);
    }

    public String failureRedirect(String errorCode) {
        return appendParameter("ssoError", errorCode);
    }

    private String appendParameter(String name, String value) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + name + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
    }

    public record SsoConfiguration(boolean enabled, String loginUrl, boolean localInternalLoginEnabled) {}
}
