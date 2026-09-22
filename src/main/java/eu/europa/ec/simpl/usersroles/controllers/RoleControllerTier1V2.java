package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.badRequest;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.conflict;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.forbidden;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.notFound;

import eu.europa.ec.simpl.api.usersroles.t1.v2.exchanges.RolesApi;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.IdentityAttributeDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RolesPagedResponseDTO;
import eu.europa.ec.simpl.common.utils.SortUtilV2;
import eu.europa.ec.simpl.usersroles.controllers.mappers.IdentityAttributeMapperTier1V2;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleMapperTier1V2;
import eu.europa.ec.simpl.usersroles.models.IdentityAttribute;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("tier1/v2")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleControllerTier1V2 implements RolesApi {

    private static final String ROLE_NOT_FOUND_MESSAGE = "Role %s not found ";

    RoleService roleService;
    RoleMapperTier1V2 roleMapper;
    IdentityAttributeMapperTier1V2 identityAttributeMapper;
    PagedResourcesAssembler<Role> pagedResourcesAssembler;

    @Override
    public RoleDTO createNewRole(RoleDTO roleDTO) {

        log.info("Creating role: {}", roleDTO);
        return switch (roleService.create(roleMapper.toRole(roleDTO))) {
            case RoleService.CreateRoleOutcome.Success outcome -> {
                log.info("Created new role {} successfully", outcome.role());
                yield roleMapper.toDto(outcome.role());
            }

            case RoleService.CreateRoleOutcome.Duplicated ignored -> throw conflict()
                    .withDetail("Role %s already exists ".formatted(roleDTO.getName()))
                    .build();

            case RoleService.CreateRoleOutcome.BuiltInRole builtInRole -> throw conflict()
                    .withDetail("Error creating role %s - trying to create a built in role"
                            .formatted(builtInRole.role().getCode()))
                    .build();
        };
    }

    @Override
    public void deleteRoleById(UUID roleId) {

        switch (roleService.delete(roleId)) {
            case RoleService.DeleteRoleOutcome.Success success -> {
                log.info("Deleted new role {} successfully", success.uuid());
            }

            case RoleService.DeleteRoleOutcome.NotFound nf -> throw notFound()
                    .withDetail("Error deleting role [%s], role does not exists".formatted(roleId))
                    .build();

            case RoleService.DeleteRoleOutcome.IsAssigned assigned -> throw conflict()
                    .withDetail(String.format("Role %s is already assigned to a user", roleId))
                    .build();
            case RoleService.DeleteRoleOutcome.BuiltInRole builtInRole -> throw conflict()
                    .withDetail(String.format("Error deleting Role %s - trying to delete a built in role", roleId))
                    .build();
        }
    }

    @Override
    public RoleDTO getRoleById(UUID roleId) {

        log.info("Retrieving role with id: {}", roleId);
        return switch (roleService.findRoleBy(new RoleService.FindByArgs.FindById(roleId))) {
            case RoleService.FindRoleByOutcome.Success outcome -> {
                log.info("Found role successfully: {}", outcome.role());
                yield roleMapper.toDto(outcome.role());
            }

            case RoleService.FindRoleByOutcome.NotFound notFound -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(roleId.toString()))
                    .build();
        };
    }

    @Override
    public List<IdentityAttributeDTO> getRoleIdentityAttributes(@Valid UUID roleId) {
        log.info("Retrieving role identity attributes with id: {}", roleId);
        return switch (roleService.findIdentityAttributesByRoleId(roleId)) {
            case RoleService.GetIdentityAttributesByRoleIdOutcome.Success success -> {
                var codes = success.identityAttributes().stream()
                        .map(IdentityAttribute::getCode)
                        .toList();
                log.info("Found {} identity attributes for role {}", codes.size(), roleId);
                yield codes.stream().map(identityAttributeMapper::toDto).toList();
            }
            case RoleService.GetIdentityAttributesByRoleIdOutcome.NotFound ignore -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(roleId.toString()))
                    .build();
        };
    }

    @Override
    public void importRoles(List<RoleDTO> roleDTO) {

        log.info("Received POST request for importing [{}] roles", roleDTO);
        switch (roleService.importRoles(roleMapper.toRoles(roleDTO))) {
            case RoleService.ImportRolesOutcome.Success success -> log.info(
                    "Imported roles successfully: {}", success.roles());
        }
    }

    @Override
    public RolesPagedResponseDTO searchRoles(
            Integer page,
            Integer pageSize,
            List<String> sort,
            String code,
            UUID id,
            String name,
            Boolean enabled,
            Boolean builtIn,
            Boolean unpaged) {

        var filter = new RoleFilter(
                StringUtils.isNotBlank(name) ? List.of(name) : Collections.emptyList(),
                id,
                null,
                null,
                code,
                enabled,
                builtIn);

        var pageable = Boolean.TRUE.equals(unpaged)
                ? Pageable.unpaged()
                : PageRequest.of(page, pageSize, SortUtilV2.toSort(sort));

        return switch (roleService.search(filter, pageable)) {
            case RoleService.SearchRolesOutcome.Success outcome -> {
                log.info("Find roles {}.", outcome.page());
                var pageModel = pagedResourcesAssembler.toModel(outcome.page());
                yield roleMapper.toRolesPagedResponseDTO(pageModel);
            }
        };
    }

    @Override
    public void updateRoleById(UUID roleId, @Valid RoleDTO roleDTO) {

        log.info("Updating role: {}", roleDTO);
        roleDTO.setId(roleId);
        var updateRoleOutcome = roleService.update(roleMapper.toRole(roleDTO));
        switch (updateRoleOutcome) {
            case RoleService.UpdateRoleOutcome.Success outcome -> log.info(
                    "Update role {} successfully", outcome.role());

            case RoleService.UpdateRoleOutcome.NotFound notFound -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(roleDTO.getName()))
                    .build();

            case RoleService.UpdateRoleOutcome.InvalidChangeNameAttempt invalidInput -> throw forbidden()
                    .withDetail("Update operation error: invalid change name attempt")
                    .build();
            case RoleService.UpdateRoleOutcome.BuiltInRole builtInRole -> throw conflict()
                    .withDetail("Update operation error: trying to update a built in role")
                    .build();
        }
    }

    @Override
    public void updateRoleIdentityAttributes(UUID roleId, List<IdentityAttributeDTO> identityAttributeDTO) {

        var attributes = identityAttributeMapper.toCodes(identityAttributeDTO);

        var outcome = roleService.replaceIdentityAttributes(roleId, attributes);

        switch (outcome) {
            case RoleService.ReplaceIdentityAttributesOutcome.Success success -> log.info(
                    "Successfully updated attributes [{}] for role [{}]", roleId, success.identityAttributes());

            case RoleService.ReplaceIdentityAttributesOutcome.RoleNotFound notFound -> throw notFound()
                    .withDetail(ROLE_NOT_FOUND_MESSAGE.formatted(roleId))
                    .build();

            case RoleService.ReplaceIdentityAttributesOutcome.InvalidIdentityAttributes invalid -> throw badRequest()
                    .withDetail(String.join(
                            "<br>",
                            invalid.errors().stream().map(Object::toString).toList()))
                    .build();
        }
    }
}
