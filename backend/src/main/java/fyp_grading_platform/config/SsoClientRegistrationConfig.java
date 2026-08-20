package fyp_grading_platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.Arrays;

@Configuration
@ConditionalOnProperty(name = "app.auth.sso.enabled", havingValue = "true")
public class SsoClientRegistrationConfig {
    @Bean
    ClientRegistrationRepository squClientRegistrationRepository(
            @Value("${app.auth.sso.registration-id:squ}") String registrationId,
            @Value("${app.auth.sso.client-id}") String clientId,
            @Value("${app.auth.sso.client-secret}") String clientSecret,
            @Value("${app.auth.sso.issuer-uri}") String issuerUri,
            @Value("${app.auth.sso.scopes:openid,profile,email}") String scopes
    ) {
        if (clientId.isBlank() || clientSecret.isBlank() || issuerUri.isBlank()) {
            throw new IllegalStateException(
                    "SQU SSO is enabled but client-id, client-secret or issuer-uri is missing"
            );
        }
        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuerUri)
                .registrationId(registrationId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope(Arrays.stream(scopes.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList())
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
