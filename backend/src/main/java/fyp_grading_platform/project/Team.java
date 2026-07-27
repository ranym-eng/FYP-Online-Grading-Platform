package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.user.StudentProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {
    @Column(nullable = false)
    private String name;
    private String section;
    @Column(nullable = false)
    private String academicYear;
    @OneToOne(optional = false)
    private Project project;
    @ManyToMany
    @JoinTable(name = "team_members")
    private Set<StudentProfile> students = new LinkedHashSet<>();
}
