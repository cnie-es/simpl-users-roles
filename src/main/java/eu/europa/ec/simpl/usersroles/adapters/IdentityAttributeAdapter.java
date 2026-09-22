package eu.europa.ec.simpl.usersroles.adapters;

import eu.europa.ec.simpl.api.authenticationprovider.v1.model.IdentityAttributeWithOwnershipDTO;
import java.util.Optional;

public interface IdentityAttributeAdapter {

    Optional<IdentityAttributeWithOwnershipDTO> findIdentityAttribute(String iaCode);
}
