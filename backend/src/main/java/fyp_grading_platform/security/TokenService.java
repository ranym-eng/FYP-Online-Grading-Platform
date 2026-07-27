package fyp_grading_platform.security;

import fyp_grading_platform.user.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {
    public String generate(User user) {
        String payload = user.getId() + ":" + user.getEmail() + ":" + user.getRole() + ":" + Instant.now().plusSeconds(86400).getEpochSecond();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
