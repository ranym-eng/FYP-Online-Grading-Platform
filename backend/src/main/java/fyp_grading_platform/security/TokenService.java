package fyp_grading_platform.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.exception.BusinessException;
import fyp_grading_platform.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long lifetimeSeconds;

    public TokenService(
            ObjectMapper objectMapper,
            @Value("${app.security.token-secret:}") String secret,
            @Value("${app.security.token-lifetime-seconds:28800}") long lifetimeSeconds
    ) {
        this.objectMapper = objectMapper;
        if (secret == null || secret.isBlank()) {
            byte[] generatedSecret = new byte[32];
            new SecureRandom().nextBytes(generatedSecret);
            this.secret = generatedSecret;
        } else {
            if (secret.length() < 32) {
                throw new IllegalArgumentException("app.security.token-secret must contain at least 32 characters");
            }
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.lifetimeSeconds = lifetimeSeconds;
    }

    public String generate(User user) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId().toString());
            payload.put("email", user.getEmail());
            payload.put("role", user.getRole().name());
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().plusSeconds(lifetimeSeconds).getEpochSecond());
            String encodedPayload = encodeJson(payload);
            String unsignedToken = header + "." + encodedPayload;
            return unsignedToken + "." + ENCODER.encodeToString(sign(unsignedToken));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create authentication token", exception);
        }
    }

    public TokenClaims parse(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) throw invalidToken();
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] providedSignature = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, providedSignature)) throw invalidToken();

            Map<String, Object> header = decodeJson(parts[0]);
            if (!"HS256".equals(header.get("alg"))) throw invalidToken();
            Map<String, Object> payload = decodeJson(parts[1]);
            long expiresAt = number(payload.get("exp"));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new BusinessException("TOKEN_EXPIRED", "Authentication token has expired");
            }
            return new TokenClaims(
                    UUID.fromString(String.valueOf(payload.get("sub"))),
                    String.valueOf(payload.get("email")),
                    UserRole.valueOf(String.valueOf(payload.get("role"))),
                    expiresAt
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private String encodeJson(Map<String, ?> value) throws Exception {
        return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private Map<String, Object> decodeJson(String value) throws Exception {
        return objectMapper.readValue(DECODER.decode(value), MAP_TYPE);
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private BusinessException invalidToken() {
        return new BusinessException("INVALID_TOKEN", "Authentication token is invalid");
    }

    public record TokenClaims(UUID subject, String email, UserRole role, long expiresAt) {}
}