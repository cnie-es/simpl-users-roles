package eu.europa.ec.simpl.usersroles.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IdentityAttribute {
    private String code;
}
