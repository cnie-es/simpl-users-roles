package eu.europa.ec.simpl.usersroles.entities;

import eu.europa.ec.simpl.common.entity.annotations.UUIDv7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name = "identity_attribute_roles")
@Accessors(chain = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class IdentityAttributeRole {

    @Id
    @UUIDv7Generator
    private UUID id;

    @Column(name = "ida_code")
    private String idaCode;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role", nullable = false)
    private RoleEntity role;

    @Column(name = "enabled")
    private Boolean enabled;
}
