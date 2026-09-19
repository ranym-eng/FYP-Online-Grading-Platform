package fyp_grading_platform.user;

import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUniversityId(String universityId);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUniversityId(String universityId);
    @Query("select distinct user from User user left join user.roles assignedRole "
            + "where assignedRole = :role or user.role = :role")
    List<User> findByRole(@Param("role") UserRole role);
    List<User> findByStatus(UserStatus status);
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String fullName, String email);
}
