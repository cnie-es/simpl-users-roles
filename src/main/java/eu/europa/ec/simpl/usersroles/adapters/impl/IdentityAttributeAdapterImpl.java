package eu.europa.ec.simpl.usersroles.adapters.impl;

import eu.europa.ec.simpl.api.authenticationprovider.v1.exchanges.IdentityAttributesApi;
import eu.europa.ec.simpl.api.authenticationprovider.v1.model.IdentityAttributeWithOwnershipDTO;
import eu.europa.ec.simpl.api.authenticationprovider.v1.model.SearchIdentityAttributesWithOwnershipFilterParameterDTO;
import eu.europa.ec.simpl.usersroles.adapters.IdentityAttributeAdapter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class IdentityAttributeAdapterImpl implements IdentityAttributeAdapter {

    private final IdentityAttributesApi identityAttributesApi;

    /**
     * Searches for an enabled and assigned to this participant identity attribute,
     * in the authentication provider by the given code.
     * <p>
     * The method builds a search filter with the provided code and queries the
     * {@code identityAttributesApi}. If one or more attributes are found, the first
     * result is returned wrapped in an {@link Optional}. If no attribute is found,
     * an empty {@link Optional} is returned.
     *
     * @param iaCode the identity attribute code to search for (must not be {@code null})
     * @return an {@link Optional} containing the first matching
     *         {@link IdentityAttributeWithOwnershipDTO} if found,
     *         or {@link Optional#empty()} if no match is found
     */
    @Override
    public Optional<IdentityAttributeWithOwnershipDTO> findIdentityAttribute(String iaCode) {
        log.info("Searching for identity attribute '{}' in authentication provider", iaCode);
        var filter = new SearchIdentityAttributesWithOwnershipFilterParameterDTO().setCode(iaCode);
        var result = identityAttributesApi.searchIdentityAttributesWithOwnership(0, 1, List.of(), filter);
        var content = result.getContent();
        var optionalResult = !content.isEmpty()
                ? Optional.of(content.getFirst())
                : Optional.<IdentityAttributeWithOwnershipDTO>empty();
        log.info("Result of searching for identity attribute '{}' in authentication provider", optionalResult);
        return optionalResult;
    }
}
