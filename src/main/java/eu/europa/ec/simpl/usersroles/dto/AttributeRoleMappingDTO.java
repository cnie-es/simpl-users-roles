package eu.europa.ec.simpl.usersroles.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AttributeRoleMappingDTO {
    @NotBlank
    private String role;

    @NotEmpty
    @Valid
    private List<@NotBlank String> identityAttributes;
}
