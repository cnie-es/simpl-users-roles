package eu.europa.ec.simpl.usersroles.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class RoleFilter {

    private List<String> name = new ArrayList<String>();
    private UUID id;
    private String description;
    private String attributeName;
    private String code;
    private Boolean enabled;
    private Boolean builtIn;
}
