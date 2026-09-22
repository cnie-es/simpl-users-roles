package eu.europa.ec.simpl.usersroles.repositories;

import static org.mockito.BDDMockito.given;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityAttributeRoleRepositoryTest {

    @Spy
    IdentityAttributeRolesRepository identityAttributeRolesRepository;

    @Test
    void existsAtLeastOne() {
        given(identityAttributeRolesRepository.count()).willReturn(1L);

        var existsAtLeastOne = identityAttributeRolesRepository.existsAtLeastOne();

        Assertions.assertThat(existsAtLeastOne)
                .as("Repository contains at least one record")
                .isTrue();
    }
}
