package fyp_grading_platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluatorProfileRepository extends JpaRepository<EvaluatorProfile, UUID> {
    Optional<EvaluatorProfile> findByUserId(UUID userId);
    List<EvaluatorProfile> findByExternal(boolean external);
}
