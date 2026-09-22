package eu.europa.ec.simpl.usersroles.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserFilter {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
}
