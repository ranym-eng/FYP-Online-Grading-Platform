package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.user.EvaluatorProfile;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "project_supervisor_assignments")
public class ProjectSupervisorAssignment extends BaseEntity {
    @ManyToOne(optional = false)
    private Project project;
    @ManyToOne(optional = false)
    private EvaluatorProfile supervisor;
    private boolean active = true;
}
