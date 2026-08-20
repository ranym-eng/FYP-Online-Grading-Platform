package fyp_grading_platform.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface IndustryInvitationRepository extends JpaRepository<IndustryInvitation, UUID> {
    Optional<IndustryInvitation> findByTokenHashAndAcceptedAtIsNull(String tokenHash);

    @Transactional
    void deleteByUserId(UUID userId);
}
