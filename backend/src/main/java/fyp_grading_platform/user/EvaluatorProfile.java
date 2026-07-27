package fyp_grading_platform.user;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "evaluator_profiles")
public class EvaluatorProfile extends BaseEntity {
    @OneToOne(optional = false)
    private User user;
    private String department;
    private String specialization;
    private String externalOrganization;
    private boolean external;
}
