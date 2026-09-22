package eu.europa.ec.simpl.usersroles.models;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Role {

    private UUID id;

    private String code;

    private String name;

    private String description;

    private Boolean builtIn;

    private Boolean enabled;

    private List<String> assignedIdentityAttributes;

    public boolean isBuiltIn() {
        return Boolean.TRUE.equals(this.builtIn);
    }
}
