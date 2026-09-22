package eu.europa.ec.simpl.usersroles.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "role")
public class RoleEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "builtin")
    private Boolean builtIn;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @PastOrPresent
    @CreationTimestamp
    @Column(name = "creation_timestamp", nullable = false)
    private Instant creationTimestamp;

    @PastOrPresent
    @UpdateTimestamp
    @Column(name = "last_update_timestamp", nullable = false)
    private Instant lastUpdateTimestamp;

    @ToString.Exclude
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IdentityAttributeRole> identityAttributeRoles;

    public boolean isBuiltIn() {
        return Boolean.TRUE.equals(this.builtIn);
    }
}
