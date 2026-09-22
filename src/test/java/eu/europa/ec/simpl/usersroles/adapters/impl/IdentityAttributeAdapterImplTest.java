package eu.europa.ec.simpl.usersroles.adapters.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.an;
import static eu.europa.ec.simpl.common.test.TestUtil.anUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import eu.europa.ec.simpl.api.authenticationprovider.v1.exchanges.IdentityAttributesApi;
import eu.europa.ec.simpl.api.authenticationprovider.v1.model.IdentityAttributeWithOwnershipDTO;
import eu.europa.ec.simpl.api.authenticationprovider.v1.model.PagedModelIdentityAttributeWithOwnershipDTO;
import eu.europa.ec.simpl.api.authenticationprovider.v1.model.SearchIdentityAttributesWithOwnershipFilterParameterDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityAttributeAdapterImplTest {

    @Mock
    private IdentityAttributesApi identityAttributesApi;

    @InjectMocks
    private IdentityAttributeAdapterImpl adapter;

    @Test
    void testFindIdentityAttributesByCodeGivenExistingIdentityAttributeWillReturnIdentityAttribute() {
        var iaCode = "ia1";
        var codes = List.of(iaCode);
        var iaId = anUUID();

        var attributes = mockSearchIdentityAttributesFromAuthenticationProviderByCode(codes);
        attributes.forEach(ia -> ia.setId(iaId));

        var result = adapter.findIdentityAttribute(iaCode);
        assertThat(result)
                .as("the result")
                .map(IdentityAttributeWithOwnershipDTO::getId)
                .hasValue(iaId);
        then(identityAttributesApi)
                .should()
                .searchIdentityAttributesWithOwnership(
                        0, 1, List.of(), new SearchIdentityAttributesWithOwnershipFilterParameterDTO().setCode(iaCode));
    }

    @Test
    void testFindIdentityAttributesByCodeGivenNotExistingIdentityAttributeWillReturnOptionalEmpty() {
        var iaCode1 = "ia1";
        var iaCode2 = "ia2";
        var codes = List.of(iaCode1);
        var iaId = anUUID();

        var attributes = mockSearchIdentityAttributesFromAuthenticationProviderByCode(codes);
        attributes.forEach(ia -> ia.setId(iaId));

        var result = adapter.findIdentityAttribute(iaCode2);
        assertThat(result).as("the result").isNotPresent();
    }

    private List<IdentityAttributeWithOwnershipDTO> mockSearchIdentityAttributesFromAuthenticationProviderByCode(
            List<String> codes) {
        var emptyResult = new PagedModelIdentityAttributeWithOwnershipDTO();
        emptyResult.setContent(List.of());

        var results = codes.stream().collect(Collectors.toMap(code -> code, code -> {
            var ia = an(IdentityAttributeWithOwnershipDTO.class);
            ia.setCode(code);
            var result = new PagedModelIdentityAttributeWithOwnershipDTO();
            result.setContent(List.of(ia));
            return result;
        }));

        given(identityAttributesApi.searchIdentityAttributesWithOwnership(any(), any(), any(), any()))
                .willAnswer(args -> {
                    var filter = args.getArgument(3, SearchIdentityAttributesWithOwnershipFilterParameterDTO.class);
                    return results.getOrDefault(filter.getCode(), emptyResult);
                });
        return results.values().stream()
                .flatMap(res -> res.getContent().stream())
                .toList();
    }
}
