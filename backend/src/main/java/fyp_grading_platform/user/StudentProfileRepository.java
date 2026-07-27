package fyp_grading_platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    boolean existsByStudentNumber(String studentNumber);
    boolean existsByEmailIgnoreCase(String email);
    Optional<StudentProfile> findByStudentNumber(String studentNumber);
    Optional<StudentProfile> findByEmailIgnoreCase(String email);
    List<StudentProfile> findByTrackCode(String trackCode);
    List<StudentProfile> findByAcademicYear(String academicYear);
    List<StudentProfile> findByCohort(String cohort);
    List<StudentProfile> findByFullNameContainingIgnoreCaseOrStudentNumberContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName,
            String studentNumber,
            String email
    );
}