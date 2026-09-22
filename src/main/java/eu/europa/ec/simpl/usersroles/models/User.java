package eu.europa.ec.simpl.usersroles.models;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class User {

    private UUID id;

    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private List<String> roles;

    private Boolean enabled;

    private String participantId;

    private String organization;
}
