package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentDomainModelTest {
    @Test
    void studentIsAcademicDataAndNotAnAuthenticatedRole() {
        assertFalse(Arrays.stream(UserRole.values()).anyMatch(role -> role.name().equals("STUDENT")));
        assertThrows(NoSuchFieldException.class, () -> StudentProfile.class.getDeclaredField("user"));
    }
}
