package fyp_grading_platform.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureCredentialGenerator {
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private final SecureRandom random = new SecureRandom();

    public String verificationCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    public String temporaryPassword() {
        char[] password = new char[16];
        password[0] = randomCharacter(UPPER);
        password[1] = randomCharacter(LOWER);
        password[2] = randomCharacter(DIGITS);
        password[3] = randomCharacter(SYMBOLS);
        for (int index = 4; index < password.length; index++) {
            password[index] = randomCharacter(ALL);
        }
        for (int index = password.length - 1; index > 0; index--) {
            int swapWith = random.nextInt(index + 1);
            char value = password[index];
            password[index] = password[swapWith];
            password[swapWith] = value;
        }
        return new String(password);
    }

    private char randomCharacter(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }
}
