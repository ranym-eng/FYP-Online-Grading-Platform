package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {
    @Column(nullable = false)
    private String title;
    @Column(length = 4000)
    private String abstractText;
    @Column(nullable = false)
    private String academicYear;
    @Column(nullable = false)
    private String status = "ACTIVE";
    @ManyToOne(optional = false)
    private Track track;
}
