package eu.europa.ec.simpl.usersroles.services.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static eu.europa.ec.simpl.common.test.TestUtil.aString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import eu.europa.ec.simpl.api.authenticationprovider.v1.model.IdentityAttributeWithOwnershipDTO;
import eu.europa.ec.simpl.usersroles.adapters.IdentityAttributeAdapter;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.services.IdentityAttributeService;
import eu.europa.ec.simpl.usersroles.services.model.InvalidOutput;
import java.util.List;
import java.util.Optional;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityAttributeServiceImplTest {

    @Mock
    IdentityAttributeRolesRepository repository;

    @Mock
    IdentityAttributeAdapter identityAttributeAdapter;

    @InjectMocks
    IdentityAttributeServiceImpl service;

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testAssignIdentityAttributes(List<String> codes) {
        service.assignIdentityAttributes(codes);
        then(repository).should().updateAssignments(codes);
    }

    @Test
    void testValidateIdentityAttributesAssignableToRolesGivenValidIdentityAttributeWillReturnSuccess() {
        var codes = List.of("ia1", "ia2");
        var code1 = validAttribute().setCode(codes.get(0));
        var code2 = validAttribute().setCode(codes.get(1));

        given(identityAttributeAdapter.findIdentityAttribute(any())).willReturn(Optional.of(code1), Optional.of(code2));

        var identityAttributeOutcome = service.validateIdentityAttributesAssignableToRoles(codes);
        assertThat(identityAttributeOutcome)
                .as("the outcome is success")
                .isInstanceOf(IdentityAttributeService.ValidateIdentityAttributeOutcome.Success.class);
    }

    @Test
    void testValidateIdentityAttributesAssignableToRolesGivenNotValidIdentityAttributeWillReturnInvalid() {
        var identityAttribute = validAttribute().setAssignableToRoles(Boolean.FALSE);
        testInvalidIdentityAttribute(identityAttribute, InvalidOutput.NotAssignableToRole.class);
    }

    @Test
    void testValidateIdentityAttributesEnabledGivenNotValidIdentityAttributeWillReturnInvalid() {
        var identityAttribute = validAttribute().setEnabled(Boolean.FALSE);
        testInvalidIdentityAttribute(identityAttribute, InvalidOutput.NotEnabled.class);
    }

    @Test
    void testValidateIdentityAttributesAssignedToParticipantGivenNotValidIdentityAttributeWillReturnInvalid() {
        var identityAttribute = validAttribute().setAssignedToParticipant(Boolean.FALSE);
        testInvalidIdentityAttribute(identityAttribute, InvalidOutput.NotAssignedToParticipant.class);
    }

    @Test
    void testValidateIdentityAttributesNotFoundGivenNotValidIdentityAttributeWillReturnInvalid() {
        testInvalidIdentityAttribute(null, InvalidOutput.NotFound.class);
    }

    private void testInvalidIdentityAttribute(
            IdentityAttributeWithOwnershipDTO identityAttribute, Class<? extends InvalidOutput> target) {
        given(identityAttributeAdapter.findIdentityAttribute(any())).willReturn(Optional.ofNullable(identityAttribute));
        var code = identityAttribute != null ? identityAttribute.getCode() : aString();
        var identityAttributeOutcome = service.validateIdentityAttributesAssignableToRoles(List.of(code));

        assertThat(identityAttributeOutcome)
                .as("the outcome is invalid")
                .isInstanceOfSatisfying(
                        IdentityAttributeService.ValidateIdentityAttributeOutcome.Invalid.class,
                        invalid -> assertThat(invalid.errors())
                                .as("the errors contains the not assignable identity attribute")
                                .satisfiesExactly(invalidOutput -> assertThat(invalidOutput)
                                        .as("the error is of type %s".formatted(target.getSimpleName()))
                                        .isExactlyInstanceOf(target)));
    }

    private IdentityAttributeWithOwnershipDTO validAttribute() {
        return a(IdentityAttributeWithOwnershipDTO.class)
                .setEnabled(Boolean.TRUE)
                .setAssignedToParticipant(Boolean.TRUE)
                .setAssignableToRoles(Boolean.TRUE);
    }
}
