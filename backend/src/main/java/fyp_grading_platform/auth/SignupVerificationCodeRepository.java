package fyp_grading_platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface SignupVerificationCodeRepository extends JpaRepository<SignupVerificationCode, UUID> {
    Optional<SignupVerificationCode> findTopByUserIdAndVerifiedAtIsNullOrderByCreatedAtDesc(UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}
