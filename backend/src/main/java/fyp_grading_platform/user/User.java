package fyp_grading_platform.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fyp_grading_platform.common.BaseEntity;
import fyp_grading_platform.common.UserRole;
import fyp_grading_platform.common.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Column
    @JsonIgnore
    private String passwordHash;

    @Column(nullable = false)
    private boolean passwordChangeRequired;

    private LocalDateTime temporaryPasswordExpiresAt;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<UserRole> roles = new LinkedHashSet<>();

    @Transient
    @JsonIgnore
    private UserRole activeRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime accessExpiresAt;

    public UserRole getRole() {
        return activeRole == null ? role : activeRole;
    }

    public UserRole getDefaultRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
        if (role != null) roles.add(role);
    }

    public Set<UserRole> getRoles() {
        LinkedHashSet<UserRole> effective = new LinkedHashSet<>(roles);
        if (role != null) effective.add(role);
        return Collections.unmodifiableSet(effective);
    }

    public void setRoles(Collection<UserRole> roles) {
        this.roles.clear();
        if (roles != null) this.roles.addAll(roles);
        if (role == null || !this.roles.contains(role)) {
            role = this.roles.isEmpty() ? null : this.roles.iterator().next();
        }
    }

    public void addRole(UserRole role) {
        if (role == null) return;
        roles.add(role);
        if (this.role == null) this.role = role;
    }

    public boolean hasRole(UserRole role) {
        return role != null && (this.role == role || roles.contains(role));
    }

    public void setActiveRole(UserRole activeRole) {
        if (activeRole != null && !hasRole(activeRole)) {
            throw new IllegalArgumentException("Active role must belong to the user");
        }
        this.activeRole = activeRole;
    }

    public boolean isIndustryOnly() {
        Set<UserRole> effective = getRoles();
        return effective.size() == 1 && effective.contains(UserRole.INDUSTRY_REPRESENTATIVE);
    }
}
