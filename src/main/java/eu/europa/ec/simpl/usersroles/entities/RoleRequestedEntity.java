package eu.europa.ec.simpl.usersroles.entities;

import eu.europa.ec.simpl.common.entity.annotations.UUIDv7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@NoArgsConstructor
@Table(name = "role_requested")
public class RoleRequestedEntity {

    @Id
    @UUIDv7Generator
    private UUID id;

    @Size(max = 50)
    @Column(name = "role", length = 50, nullable = false)
    private String role;

    @Size(max = 100)
    @Column(name = "requested_by", length = 100, nullable = false)
    private String requestedBy;

    @Column(name = "approved", nullable = false)
    private Boolean approved;

    @PastOrPresent
    @CreationTimestamp
    @Column(name = "requested_timestamp", nullable = false)
    private Instant requestedTimestamp;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_request_id", nullable = false)
    private RoleRequestEntity roleRequest;
}
