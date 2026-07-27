package fyp_grading_platform.project;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository extends JpaRepository<Track, UUID> {
    Optional<Track> findByCode(String code);
    boolean existsByCode(String code);
}
