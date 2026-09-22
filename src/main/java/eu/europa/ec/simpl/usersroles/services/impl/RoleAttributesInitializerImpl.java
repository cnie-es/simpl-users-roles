package eu.europa.ec.simpl.usersroles.services.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.usersroles.dto.AttributeRoleMappingDTO;
import eu.europa.ec.simpl.usersroles.properties.DBSeedingProperties;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.services.RoleAttributesInitializer;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@ConditionalOnProperty(value = "database-seeding.role-attributes-mapping.enabled", havingValue = "true")
public class RoleAttributesInitializerImpl implements RoleAttributesInitializer {

    private final IdentityAttributeRolesRepository identityAttributeRolesRepository;
    private final RoleService roleService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final DBSeedingProperties props;
    private final Validator validator;

    public RoleAttributesInitializerImpl(
            IdentityAttributeRolesRepository identityAttributeRolesRepository,
            RoleService roleService,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            DBSeedingProperties props,
            Validator validator) {
        this.identityAttributeRolesRepository = identityAttributeRolesRepository;
        this.roleService = roleService;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.props = props;
        this.validator = validator;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        if (identityAttributeRolesRepository.existsAtLeastOne()) {
            log.warn("Role-attribute mappings already exist. Skipping role-attribute mapping initialization");
            return;
        }

        var mappings = getAttributeRoleMapping();
        if (mappings.isEmpty()) {
            log.warn("No role-attribute mapping found. Check seeding file");
            return;
        }
        log.info("Successfully loaded {} role-attribute mappings", mappings.size());

        mappings.forEach(mapping ->
                roleService.preAssignIdentityAttributesToRole(mapping.getIdentityAttributes(), mapping.getRole()));

        log.info("Role-attribute mappings initialization completed successfully");
    }

    private List<AttributeRoleMappingDTO> getAttributeRoleMapping() {
        var seedingFilePath = props.roleAttributesMapping().filePath();
        log.info("Reading role-attributes mappings from {}", seedingFilePath);
        try (InputStream is = resourceLoader.getResource(seedingFilePath).getInputStream()) {
            List<AttributeRoleMappingDTO> mappings = objectMapper.readValue(is, new TypeReference<>() {});
            return validateMappings(mappings);
        } catch (IOException e) {
            log.error("Error reading mappings from file: {}", seedingFilePath, e);
        }
        return Collections.emptyList();
    }

    private List<AttributeRoleMappingDTO> validateMappings(List<AttributeRoleMappingDTO> mappings) {
        var validMappings = new ArrayList<AttributeRoleMappingDTO>();
        Map<AttributeRoleMappingDTO, Set<ConstraintViolation<AttributeRoleMappingDTO>>> errors =
                new ConcurrentHashMap<>();

        for (AttributeRoleMappingDTO mapping : mappings) {
            var violations = validator.validate(mapping);
            if (violations.isEmpty()) {
                validMappings.add(mapping);
            } else {
                errors.put(mapping, violations);
            }
        }

        if (!errors.isEmpty()) {
            errors.forEach(this::logMappingViolations);
            throw new ConstraintViolationException(
                    errors.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        }
        return validMappings;
    }

    private void logMappingViolations(
            AttributeRoleMappingDTO mapping, Set<ConstraintViolation<AttributeRoleMappingDTO>> violations) {
        violations.forEach(v -> log.error("Validation failed for mapping: {} - {}", mapping, v.getMessage()));
    }
}
