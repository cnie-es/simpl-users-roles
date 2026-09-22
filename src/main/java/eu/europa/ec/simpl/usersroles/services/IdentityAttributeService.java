package eu.europa.ec.simpl.usersroles.services;

import eu.europa.ec.simpl.usersroles.services.model.InvalidOutput;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface IdentityAttributeService {

    @Transactional
    void assignIdentityAttributes(@NotNull List<@NotBlank String> identityAttributeCodes);

    ValidateIdentityAttributeOutcome validateIdentityAttributesAssignableToRoles(
            @NotNull List<@NotBlank String> identityAttributesCode);

    sealed interface ValidateIdentityAttributeOutcome {

        record Success() implements ValidateIdentityAttributeOutcome {}

        record Invalid(List<InvalidOutput> errors) implements ValidateIdentityAttributeOutcome {}
    }
}
