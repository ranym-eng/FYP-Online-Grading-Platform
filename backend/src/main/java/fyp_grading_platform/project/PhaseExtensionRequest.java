package fyp_grading_platform.project;

import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.ExtensionRequestStatus;
import fyp_grading_platform.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "phase_extension_requests")
public class PhaseExtensionRequest extends BaseEntity {
    @ManyToOne(optional = false)
    private Phase phase;

    @ManyToOne(optional = false)
    private User requester;

    @Column(nullable = false, length = 2000)
    private String reason;

    private LocalDateTime requestedDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExtensionRequestStatus status = ExtensionRequestStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne
    private User reviewedBy;

    private LocalDateTime extendedDeadline;

    @Column(length = 2000)
    private String adminComment;
}
