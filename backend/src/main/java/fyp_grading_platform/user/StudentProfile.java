package fyp_grading_platform.user;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_profiles", uniqueConstraints = @UniqueConstraint(name = "uk_student_number", columnNames = "student_number"))
public class StudentProfile extends BaseEntity {
    @OneToOne(optional = false)
    private User user;

    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(nullable = false)
    private String academicYear;

    private String trackCode;
    private String level;
}
