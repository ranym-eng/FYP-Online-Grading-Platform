package fyp_grading_platform.user;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_student_number", columnNames = "student_number"),
        @UniqueConstraint(name = "uk_student_email", columnNames = "email")
})
public class StudentProfile extends BaseEntity {
    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(name = "full_name")
    private String fullName;

    private String email;

    private String cohort;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "track_code")
    private String trackCode;

    private String level;
}