package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "projects", uniqueConstraints = @UniqueConstraint(name = "uk_project_number", columnNames = "project_number"))
public class Project extends BaseEntity {
    @Column(name = "project_number")
    private String projectNumber;
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