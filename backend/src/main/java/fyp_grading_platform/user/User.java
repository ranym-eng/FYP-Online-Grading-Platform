package fyp_grading_platform.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "app_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_user_university_id", columnNames = "university_id")
})
public class User extends BaseEntity {
    @Column(name = "university_id", nullable = false)
    private String universityId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String passwordHash;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime accessExpiresAt;
}
