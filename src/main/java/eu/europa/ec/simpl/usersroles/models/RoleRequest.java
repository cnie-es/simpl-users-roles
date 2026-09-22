package eu.europa.ec.simpl.usersroles.models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Accessors(chain = true)
public class RoleRequest {

    UUID id;

    @NotNull String createdBy;

    String reviewedBy;

    @NotNull RoleRequestStatusEnum status;

    @NotEmpty(message = "At least one role must be requested")
    Set<@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 50) String> rolesRequested;

    Set<String> rolesAssigned;

    Instant creationTimestamp;
    Instant lastUpdateTimestamp;
}
