package fyp_grading_platform.audit;

import fyp_grading_platform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {
    private UUID userId;
    private String action;
    private String entityType;
    private UUID entityId;
    @Column(length = 4000)
    private String oldValue;
    @Column(length = 4000)
    private String newValue;
    private String ipAddress;
    private String userAgent;
}
