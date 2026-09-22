package eu.europa.ec.simpl.usersroles.services.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.usersroles.dto.AttributeRoleMappingDTO;
import eu.europa.ec.simpl.usersroles.properties.DBSeedingProperties;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@ExtendWith(MockitoExtension.class)
class RoleAttributesInitializerImplTest {

    @Spy
    private IdentityAttributeRolesRepository identityAttributeRolesRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DBSeedingProperties props;

    @Mock
    private DBSeedingProperties.RoleAttributesMapping roleAttributesMapping;

    @Mock
    private Validator validator;

    @Mock
    private Resource resource;

    @InjectMocks
    private RoleAttributesInitializerImpl roleAttributesInitializer;

    @Test
    void testSkipInitializationWhenMappingsAlreadyExist() {
        // given
        given(identityAttributeRolesRepository.existsAtLeastOne()).willReturn(true);

        // when
        roleAttributesInitializer.init();

        // then
        verify(resourceLoader, never()).getResource(any());
        verify(roleService, never()).preAssignIdentityAttributesToRole(any(), any());
    }

    @Test
    void testInitializeRoleAttributeMappingsSuccessfully() throws IOException {
        // given
        given(props.roleAttributesMapping()).willReturn(roleAttributesMapping);
        given(identityAttributeRolesRepository.existsAtLeastOne()).willReturn(false);
        given(roleAttributesMapping.filePath()).willReturn("test-path");
        given(resourceLoader.getResource("test-path")).willReturn(resource);

        AttributeRoleMappingDTO mapping = new AttributeRoleMappingDTO();
        mapping.setRole("ROLE_TEST");
        mapping.setIdentityAttributes(List.of("attr1", "attr2"));
        List<AttributeRoleMappingDTO> mappings = List.of(mapping);

        given(resource.getInputStream()).willReturn(new ByteArrayInputStream("{}".getBytes()));
        given(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .willReturn(mappings);
        given(validator.validate(any())).willReturn(Set.of());

        // when
        roleAttributesInitializer.init();

        // then
        verify(roleService).preAssignIdentityAttributesToRole(mapping.getIdentityAttributes(), mapping.getRole());
    }

    @Test
    void testHandleEmptyMappingsGracefully() throws IOException {
        // given
        given(props.roleAttributesMapping()).willReturn(roleAttributesMapping);
        given(identityAttributeRolesRepository.existsAtLeastOne()).willReturn(false);
        given(roleAttributesMapping.filePath()).willReturn("test-path");
        given(resourceLoader.getResource("test-path")).willReturn(resource);
        given(resource.getInputStream()).willReturn(new ByteArrayInputStream("{}".getBytes()));
        given(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .willReturn(List.of());

        // when
        roleAttributesInitializer.init();

        // then
        verify(roleService, never()).preAssignIdentityAttributesToRole(any(), any());
    }

    @Test
    void testHandleIOExceptionGracefully() throws IOException {
        // given
        given(props.roleAttributesMapping()).willReturn(roleAttributesMapping);
        given(identityAttributeRolesRepository.existsAtLeastOne()).willReturn(false);
        given(roleAttributesMapping.filePath()).willReturn("test-path");
        given(resourceLoader.getResource("test-path")).willReturn(resource);
        given(resource.getInputStream()).willThrow(new IOException("Test exception"));

        // when
        roleAttributesInitializer.init();

        // then
        verify(roleService, never()).preAssignIdentityAttributesToRole(any(), any());
    }

    @Test
    void testSkipInvalidMappings() throws IOException {
        // given
        given(props.roleAttributesMapping()).willReturn(roleAttributesMapping);
        given(identityAttributeRolesRepository.existsAtLeastOne()).willReturn(false);
        given(roleAttributesMapping.filePath()).willReturn("test-path");
        given(resourceLoader.getResource("test-path")).willReturn(resource);

        AttributeRoleMappingDTO validMapping = new AttributeRoleMappingDTO();
        validMapping.setRole("ROLE_VALID");
        validMapping.setIdentityAttributes(List.of("attr1"));

        AttributeRoleMappingDTO invalidMapping = new AttributeRoleMappingDTO();
        invalidMapping.setRole("ROLE_INVALID");

        List<AttributeRoleMappingDTO> mappings = List.of(validMapping, invalidMapping);

        given(resource.getInputStream()).willReturn(new ByteArrayInputStream("{}".getBytes()));
        given(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .willReturn(mappings);
        given(validator.validate(validMapping)).willReturn(Set.of());
        given(validator.validate(invalidMapping)).willReturn(Set.of(mock(ConstraintViolation.class)));

        // when/then
        assertThrows(
                ConstraintViolationException.class,
                () -> roleAttributesInitializer.init(),
                "Should throw ConstraintViolationException when there are invalid mappings");
        verify(roleService, never()).preAssignIdentityAttributesToRole(any(), any());
    }
}
