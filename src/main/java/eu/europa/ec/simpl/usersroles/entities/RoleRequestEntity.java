package eu.europa.ec.simpl.usersroles.entities;

import eu.europa.ec.simpl.common.entity.annotations.UUIDv7Generator;
import eu.europa.ec.simpl.usersroles.models.RoleRequestStatusEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "role_request")
public class RoleRequestEntity {

    @Id
    @UUIDv7Generator
    private UUID id;

    @ColumnTransformer(read = "lower(user_email)", write = "lower(?)")
    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoleRequestStatusEnum status;

    @PastOrPresent
    @CreationTimestamp
    @Column(name = "creation_timestamp", nullable = false)
    private Instant creationTimestamp;

    @PastOrPresent
    @UpdateTimestamp
    @Column(name = "last_update_timestamp", nullable = false)
    private Instant lastUpdateTimestamp;

    @Size(max = 100)
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @ToString.Exclude
    @OneToMany(mappedBy = "roleRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RoleRequestedEntity> rolesRequested;

    public void addRoleRequested(RoleRequestedEntity roleRequested) {
        if (rolesRequested == null) {
            rolesRequested = new HashSet<>();
        }
        roleRequested.setRoleRequest(this);
        rolesRequested.add(roleRequested);
    }
}
