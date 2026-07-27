package fyp_grading_platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    boolean existsByStudentNumber(String studentNumber);
    Optional<StudentProfile> findByUserId(UUID userId);
    List<StudentProfile> findByTrackCode(String trackCode);
    List<StudentProfile> findByAcademicYear(String academicYear);
}
