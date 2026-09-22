package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.badRequest;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.conflict;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.forbidden;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.notFound;

import eu.europa.ec.simpl.api.usersroles.v1.exchanges.RolesApi;
import eu.europa.ec.simpl.api.usersroles.v1.model.PageResponseRoleDTO;
import eu.europa.ec.simpl.api.usersroles.v1.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.v1.model.SearchRolesFilterParameterDTO;
import eu.europa.ec.simpl.common.utils.UUIDUtil;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleMapperV1;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("v1")
public class RoleControllerV1 implements RolesApi {

    private static final String ROLE_NOT_FOUND_MESSAGE = "Role %s not found ";

    private final RoleService roleService;

    private final RoleMapperV1 roleMapper;

    public RoleControllerV1(RoleService roleService, RoleMapperV1 roleMapper) {
        this.roleMapper = roleMapper;
        this.roleService = roleService;
    }

    @Override
    public RoleDTO create(RoleDTO keycloakRoleDTO) {
        log.info("Received POST request for role: {}", keycloakRoleDTO);
        return switch (roleService.create(roleMapper.toRole(keycloakRoleDTO))) {
            case RoleService.CreateRoleOutcome.Success outcome -> {
                log.info("Create role {}.", outcome.role());
                yield roleMapper.toDto(outcome.role());
            }
            case RoleService.CreateRoleOutcome.Duplicated ignored -> throw conflict()
                    .withDetail("Role %s already exists ".formatted(keycloakRoleDTO.getName()))
                    .build();

            case RoleService.CreateRoleOutcome.BuiltInRole builtInRole -> throw conflict()
                    .withDetail("Cannot create Role %s as built in"
                            .formatted(builtInRole.role().getCode()))
                    .build();
        };
    }

    @Override
    public void deleteAttributeFromRole(UUID roleId, String attributeCode) {
        log.info("Received DELETE request for attribute: {} from role: {} ", attributeCode, roleId);
        switch (roleService.removeAttributeForRole(attributeCode, roleId)) {
            case RoleService.RemoveAttributeForRoleOutcome.Success ignored -> log.info(
                    "Role attribute {} removed for role {}.", attributeCode, roleId);
            case RoleService.RemoveAttributeForRoleOutcome.NotFound outcome -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(outcome.uuid()))
                    .build();
        }
    }

    @Override
    public void deleteCredential(UUID roleId) {
        log.info("Received DELETE request for role with id: {}", roleId);
        switch (roleService.delete(roleId)) {
            case RoleService.DeleteRoleOutcome.Success ignored -> log.info("Role {} is removed.", roleId);
            case RoleService.DeleteRoleOutcome.NotFound outcome -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(outcome.uuid()))
                    .build();
            case RoleService.DeleteRoleOutcome.IsAssigned assigned -> throw conflict()
                    .withDetail(String.format("Role %s is already assigned to a user", roleId))
                    .build();
            case RoleService.DeleteRoleOutcome.BuiltInRole builtInRole -> {
                throw conflict()
                        .withDetail(String.format("Error deleting role Role %s - cannot delete built in roles", roleId))
                        .build();
            }
        }
    }

    @Override
    public void duplicateIdentityAttributeToAnOtherRole(UUID id, String body) {
        var uuidTarget = UUIDUtil.retrieveValidUUIDFromString(body);
        switch (roleService.duplicateIdentityAttributeToAnOtherRole(id, uuidTarget)) {
            case RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.Success ignored -> log.info(
                    "Role {} is duplicated on target {}.", id, uuidTarget);
            case RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.NotFound outcome -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(outcome.uuid()))
                    .build();
        }
    }

    @Override
    public RoleDTO findById(UUID id) {
        log.info("Received GET request for role with id: {}", id);
        return switch (roleService.findRoleBy(new RoleService.FindByArgs.FindById(id))) {
            case RoleService.FindRoleByOutcome.Success outcome -> {
                log.info("Find role {}.", outcome.role());
                yield roleMapper.toDto(outcome.role());
            }
            case RoleService.FindRoleByOutcome.NotFound ignored -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(id.toString()))
                    .build();
        };
    }

    @Override
    public void importRoles(List<RoleDTO> keycloakRoleDTO) {

        log.info("Received POST request for importing [{}] roles", keycloakRoleDTO);
        switch (roleService.importRoles(roleMapper.toRoles(keycloakRoleDTO))) {
            case RoleService.ImportRolesOutcome.Success success -> log.info(
                    "Imported roles successfully: {}", success.roles());
        }
    }

    @Override
    public void replaceIdentityAttributes(UUID id, List<String> requestBody) {
        switch (roleService.replaceIdentityAttributes(id, requestBody)) {
            case RoleService.ReplaceIdentityAttributesOutcome.Success ignored -> {}
            case RoleService.ReplaceIdentityAttributesOutcome.RoleNotFound outcome -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(outcome.uuid().toString()))
                    .build();
            case RoleService.ReplaceIdentityAttributesOutcome.InvalidIdentityAttributes outcome -> throw badRequest()
                    .withDetail(String.join(
                            "<br>",
                            outcome.errors().stream().map(Object::toString).toList()))
                    .build();
        }
    }

    @Override
    public PageResponseRoleDTO searchRoles(
            Integer page, Integer size, List<String> sort, SearchRolesFilterParameterDTO filter) {

        var roleFilter = roleMapper.toSearchRolesFilterParameter(filter);

        var pageable = PageRequest.of(page, size, sortBy(sort));

        return switch (roleService.search(roleFilter, pageable)) {
            case RoleService.SearchRolesOutcome.Success outcome -> {
                log.info("Find roles {}.", outcome.page());
                yield roleMapper.toRolesPagedResponseDTO(outcome.page());
            }
        };
    }

    @Override
    public RoleDTO update(UUID roleId, RoleDTO roleDTO) {
        log.info("Received PUT request for role: {}", roleDTO);
        return switch (roleService.update(roleMapper.toRole(roleDTO))) {
            case RoleService.UpdateRoleOutcome.Success outcome -> {
                log.info("Update roles {}.", outcome.role());
                yield roleMapper.toDto(outcome.role());
            }
            case RoleService.UpdateRoleOutcome.NotFound outcome -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(outcome.uuid().toString()))
                    .build();
            case RoleService.UpdateRoleOutcome.InvalidChangeNameAttempt outcome -> throw forbidden()
                    .withDetail("Update operation error: invalid change name attempt")
                    .build();
            case RoleService.UpdateRoleOutcome.BuiltInRole builtInRole -> throw conflict()
                    .withDetail("Update operation error: invalid built in role update attempt")
                    .build();
        };
    }

    private static Sort sortBy(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }

        Sort result = Sort.unsorted();
        int i = 0;
        int sortSize = sort.size();

        while (i < sortSize) {
            String property = requireProperty(sort, i);

            Sort.Direction direction = directionFromNextToken(sort, i);
            if (hasDirectionTokenAfter(sort, i)) {
                i += 2;
            } else {
                i += 1;
            }

            result = result.and(Sort.by(direction, property));
        }

        return result;
    }

    private static String requireProperty(List<String> sort, int index) {
        String property = sort.get(index);

        if (StringUtils.isBlank(property)) {
            throw new IllegalArgumentException("Error - null or blank sort property at index " + index);
        }

        if (isDirectionToken(property)) {
            throw new IllegalArgumentException("Sort direction without property at index " + index + ": " + property);
        }

        return property;
    }

    private static Sort.Direction directionFromNextToken(List<String> sort, int index) {
        if (!hasDirectionTokenAfter(sort, index)) {
            return Sort.Direction.ASC;
        }
        String next = sort.get(index + 1);
        return "desc".equalsIgnoreCase(next) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    private static boolean hasDirectionTokenAfter(List<String> sort, int index) {
        return index + 1 < sort.size() && isDirectionToken(sort.get(index + 1));
    }

    private static boolean isDirectionToken(String token) {
        return "asc".equalsIgnoreCase(token) || "desc".equalsIgnoreCase(token);
    }
}
