package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUniversityId(String universityId);
    List<User> findByRole(UserRole role);
    List<User> findByStatus(UserStatus status);
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email);
}
