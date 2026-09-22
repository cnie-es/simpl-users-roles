package eu.europa.ec.simpl.usersroles.services.impl;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.entities.IdentityAttributeRole;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.repositories.RoleRepository;
import eu.europa.ec.simpl.usersroles.repositories.specifications.RoleSpecification;
import eu.europa.ec.simpl.usersroles.services.IdentityAttributeService;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.mappers.RoleMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private static final String ROLE_DOES_NOT_EXIST_LOG_MESSAGE = "Role does not exist {}.";

    private final IdentityAttributeRolesRepository identityAttributeRolesRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final IdentityAttributeService identityAttributeService;
    private final RoleAdapter roleAdapter;

    @Override
    @Transactional
    public ReplaceIdentityAttributesOutcome replaceIdentityAttributes(
            @NotNull UUID roleId, @NotEmpty List<String> attributesCode) {
        log.info("Start replace identity attributes. RoleId: {}", roleId);
        var roleEntityOptional = roleRepository.findById(roleId);

        if (roleEntityOptional.isEmpty()) {
            log.warn("Role with id [{}] not found", roleId);
            return new ReplaceIdentityAttributesOutcome.RoleNotFound(roleId);
        }

        var roleFound = roleEntityOptional.get();

        log.info("Found role {}", roleFound);

        log.debug("Start validationg identity attributes {} ", attributesCode);
        var identityAttributeOutcome =
                identityAttributeService.validateIdentityAttributesAssignableToRoles(attributesCode);

        switch (identityAttributeOutcome) {
            case IdentityAttributeService.ValidateIdentityAttributeOutcome.Success ignored -> log.info(
                    "Validated identity attributes {} successfully", attributesCode);
            case IdentityAttributeService.ValidateIdentityAttributeOutcome.Invalid outcome -> {
                log.warn("Found errors during identity attributes validation for codes: {}", attributesCode);
                return new ReplaceIdentityAttributesOutcome.InvalidIdentityAttributes(outcome.errors());
            }
        }

        identityAttributeRolesRepository.deleteByRole_Code(roleFound.getCode());
        identityAttributeRolesRepository.flush();
        var savedAttributes = identityAttributeRolesRepository.saveAll(attributesCode.stream()
                .map(idaCode -> new IdentityAttributeRole()
                        .setRole(roleFound)
                        .setIdaCode(idaCode)
                        .setEnabled(Boolean.TRUE))
                .toList());

        var savedCodes =
                savedAttributes.stream().map(IdentityAttributeRole::getIdaCode).collect(Collectors.joining(","));

        log.info("Assigned identity attributes [{}] to role {} ", savedCodes, roleFound.getCode());
        return new ReplaceIdentityAttributesOutcome.Success(
                savedAttributes.stream().map(IdentityAttributeRole::getIdaCode).toList());
    }

    @Override
    @Transactional
    public RemoveAttributeForRoleOutcome removeAttributeForRole(@NotBlank String attributeCode, @NotNull UUID roleId) {

        log.info("Start remove attribute for role {}, attribute to remove: {}", roleId, attributeCode);

        var findByIdOutcome = findRoleBy(new FindByArgs.FindById(roleId));

        Role role;

        switch (findByIdOutcome) {
            case FindRoleByOutcome.NotFound notFound -> {
                log.info("Error removing attribute for role [ID: {}] - role not found", roleId);
                return new RemoveAttributeForRoleOutcome.NotFound(roleId);
            }
            case FindRoleByOutcome.Success success -> role = success.role();
        }

        log.info("Found role with details [{}]", role);

        var roleCode = role.getCode();
        if (identityAttributeRolesRepository.deleteByRole_CodeAndIdaCode(roleCode, attributeCode) == 0) {
            log.warn("Identity Attribute {} is not assigned to role {}.", attributeCode, roleCode);
            return new RemoveAttributeForRoleOutcome.NotFound(roleId);
        }

        return new RemoveAttributeForRoleOutcome.Success();
    }

    @Override
    @Transactional
    public DuplicateIdentityAttributeToAnOtherRoleOutcome duplicateIdentityAttributeToAnOtherRole(
            @NotNull UUID sourceRoleId, @NotNull UUID destinationRoleId) {

        log.info("Start duplicate identity attributes from {} to {} ", sourceRoleId, destinationRoleId);

        var sourceRoleOpt = roleRepository.findById(sourceRoleId);

        if (sourceRoleOpt.isEmpty()) {
            log.warn("Source role does not exist {}.", sourceRoleId);
            return new DuplicateIdentityAttributeToAnOtherRoleOutcome.NotFound(sourceRoleId);
        }

        var destRoleOpt = roleRepository.findById(destinationRoleId);

        if (destRoleOpt.isEmpty()) {
            log.warn("Destination role does not exist {}.", destinationRoleId);
            return new DuplicateIdentityAttributeToAnOtherRoleOutcome.NotFound(destinationRoleId);
        }

        var sourceRole = sourceRoleOpt.get();

        var destRole = destRoleOpt.get();

        log.info("Found source role with details: {}", sourceRole);

        log.info("Found destination role with details: {}", destRole);

        var sourceRoleIDAs = getEnabledAttrs(sourceRole.getCode());

        var destinationRoleIDAs = getEnabledAttrs(destRole.getCode());

        var missingAttributes = getMissingAttributes(sourceRoleIDAs, destinationRoleIDAs);

        missingAttributes.forEach(idaCode -> identityAttributeRolesRepository.save(new IdentityAttributeRole()
                .setRole(destRole)
                .setIdaCode(idaCode)
                .setEnabled(Boolean.TRUE)));

        log.info("Missing attributes {} for role {}.", missingAttributes, destinationRoleId);
        log.info("Duplicated attributes {} for role {}.", destinationRoleIDAs, destinationRoleId);
        return new DuplicateIdentityAttributeToAnOtherRoleOutcome.Success();
    }

    /**
     * This method compares the identity attribute codes between two roles and identifies
     * which attributes are present in the source role but absent in the destination role.
     *
     * @param sourceRoleIDAs      A list of IdentityAttributeRole associated with the source role.
     * @param destinationRoleIDAs A list of IdentityAttributeRole associated with the destination role.
     * @return A list of String representing the identity attribute codes that are present in the source role
     *         but missing from the destination role.
     */
    private static List<String> getMissingAttributes(
            List<IdentityAttributeRole> sourceRoleIDAs, List<IdentityAttributeRole> destinationRoleIDAs) {

        log.info("Found missing attributes from source [{}] to destination [{}]", sourceRoleIDAs, destinationRoleIDAs);

        var sourceIdaCodes =
                sourceRoleIDAs.stream().map(IdentityAttributeRole::getIdaCode).toList();

        var destinationIdaCodesSet = destinationRoleIDAs.stream()
                .map(IdentityAttributeRole::getIdaCode)
                .collect(Collectors.toSet());

        return sourceIdaCodes.stream()
                .filter(idaCode -> !destinationIdaCodesSet.contains(idaCode))
                .toList();
    }

    @Override
    public FindRoleByOutcome findRoleBy(@NotNull FindByArgs findByArgs) {

        log.info("Retrieving role with args: {}", findByArgs);

        return switch (findByArgs) {
            case FindByArgs.FindById findById -> roleRepository
                    .findById(findById.id())
                    .<FindRoleByOutcome>map(entity -> {
                        log.info("Retrieved role with id [{}] successfully, role details: [{}]", findById.id(), entity);
                        return new FindRoleByOutcome.Success(
                                roleMapper.toRole(entity, getEnabledAttrs(entity.getCode())));
                    })
                    .orElseGet(() -> new FindRoleByOutcome.NotFound(new RoleIdentifier.Id(findById.id())));

            case FindByArgs.FindByCode findByCode -> roleRepository
                    .findByCode(findByCode.code())
                    .<FindRoleByOutcome>map(entity -> {
                        log.info(
                                "Retrieved role with code [{}] successfully, role details: [{}]",
                                findByCode.code(),
                                entity);
                        return new FindRoleByOutcome.Success(
                                roleMapper.toRole(entity, getEnabledAttrs(entity.getCode())));
                    })
                    .orElseGet(() -> new FindRoleByOutcome.NotFound(new RoleIdentifier.Code(findByCode.code())));
        };
    }

    @Override
    public FindRolesByOutcome findRolesBy(FindRolesByArgs findByArgs) {

        List<Role> roles;

        switch (findByArgs) {
            case FindRolesByArgs.FindByIds findByIds -> {
                if (findByIds.roleIds().isEmpty()) {
                    return new FindRolesByOutcome.Success(List.of());
                }

                roles = roleRepository.findByIdIn(findByIds.roleIds()).stream()
                        .map(entity -> roleMapper.toRole(entity, getEnabledAttrs(entity.getCode())))
                        .toList();

                var foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());

                var missingIds = findByIds.roleIds().stream()
                        .filter(id -> !foundIds.contains(id))
                        .toList();

                if (!missingIds.isEmpty()) {
                    log.warn(
                            "Error finding role by ids: {}, some roles were not found: {}",
                            findByIds.roleIds(),
                            missingIds);
                    return new FindRolesByOutcome.NotFound(
                            roles,
                            missingIds.stream()
                                    .<RoleIdentifier>map(RoleIdentifier.Id::new)
                                    .toList());
                }
            }
            case FindRolesByArgs.FindByCodes findByCodes -> {
                if (findByCodes.codes().isEmpty()) {
                    return new FindRolesByOutcome.Success(List.of());
                }

                roles = roleRepository.findByCodeIn(findByCodes.codes()).stream()
                        .map(entity -> roleMapper.toRole(entity, getEnabledAttrs(entity.getCode())))
                        .toList();

                var foundCodes = roles.stream().map(Role::getCode).collect(Collectors.toSet());

                var missingCodes = findByCodes.codes().stream()
                        .filter(code -> !foundCodes.contains(code))
                        .toList();

                if (!missingCodes.isEmpty()) {
                    log.warn(
                            "Error finding role by codes: {}, some role codes were not found: {}",
                            findByCodes.codes(),
                            missingCodes);
                    return new FindRolesByOutcome.NotFound(
                            roles,
                            missingCodes.stream()
                                    .<RoleIdentifier>map(RoleIdentifier.Code::new)
                                    .toList());
                }
            }
        }

        return new FindRolesByOutcome.Success(roles);
    }

    @Override
    public GetIdentityAttributesByRoleIdOutcome findIdentityAttributesByRoleId(@NotNull UUID roleId) {
        log.info("Retrieving identity attributes for role {}", roleId);

        var findByIdOutcome = findRoleBy(new FindByArgs.FindById(roleId));

        Role role;

        switch (findByIdOutcome) {
            case FindRoleByOutcome.NotFound notFound -> {
                log.warn("Error finding identity attributes - role with id [{}] not found", roleId);
                return new GetIdentityAttributesByRoleIdOutcome.NotFound(roleId);
            }
            case FindRoleByOutcome.Success success -> {
                log.warn("finding identity attributes - role [{}] found", success.role());
                role = success.role();
            }
        }

        log.debug("Found role {} for id {}", role.getCode(), roleId);

        var identityAttributes = roleMapper.toIdentityAttributeList(getEnabledAttrs(role.getCode()));

        log.info("Retrieved {} identity attributes for role {}", identityAttributes.size(), roleId);
        return new GetIdentityAttributesByRoleIdOutcome.Success(identityAttributes);
    }

    @Override
    @Transactional
    public PreAssignIdentityAttributesToRoleOutcome preAssignIdentityAttributesToRole(
            @NotEmpty Collection<String> idaCodes, @NotBlank String roleCode) {

        var roleOpt = roleRepository.findByCode(roleCode);

        if (roleOpt.isEmpty()) {
            log.warn(
                    "Error Pre-assigning identity attributes for role {} to {} - Error: role not found",
                    roleCode,
                    idaCodes);
            return new PreAssignIdentityAttributesToRoleOutcome.RoleNotFound(roleCode);
        }

        log.info("Pre-assigning identity attributes for role {} to {}", roleCode, idaCodes);
        var mappings = idaCodes.stream()
                .distinct()
                .map(idaCode -> new IdentityAttributeRole()
                        .setRole(roleOpt.get())
                        .setIdaCode(idaCode)
                        .setEnabled(Boolean.FALSE))
                .toList();
        identityAttributeRolesRepository.saveAll(mappings);

        log.info("Successfully pre-assigning identity attributes for role {} to {}", roleCode, idaCodes);

        return new PreAssignIdentityAttributesToRoleOutcome.Success();
    }

    @Override
    @Transactional
    public CreateRoleOutcome create(@Valid @NotNull Role role) {
        return doCreate(role);
    }

    /**
     *
     * @param roles the collection of roles to be imported
     * @return an outcome with success for successfully importing, failure if at least one of importing fails
     */
    @Override
    @Transactional
    public ImportRolesOutcome importRoles(@Valid @NotNull Collection<@Valid Role> roles) {

        log.info("Importing roles {}", roles);

        var importedRoles = new ArrayList<Role>();

        for (Role role : roles) {

            try {

                switch (doCreate(role)) {
                    case CreateRoleOutcome.Success success -> {
                        log.info("Successfully imported role {}", role.getCode());
                        importedRoles.add(role);
                    }
                    case CreateRoleOutcome.Duplicated duplicated -> log.warn(
                            "Duplicated role {} - import will be ignored", role.getCode());

                    case CreateRoleOutcome.BuiltInRole builtInRole -> {
                        log.warn("Built in role {} - import will be ignored", role.getCode());
                    }
                }

            } catch (KeycloakException e) {
                log.error("Error importing role [{}] - Error:", role.getCode(), e);
                rollbackImportedRoles(importedRoles);
                throw e;
            }
        }

        log.info("Roles imported successfully: [{}]", importedRoles);

        return new ImportRolesOutcome.Success(importedRoles);
    }

    @Override
    @Transactional
    public UpdateRoleOutcome update(@Valid @NotNull Role role) {

        log.info("Updating role {}", role);

        try {

            var roleOpt = roleRepository.findById(role.getId());

            if (roleOpt.isEmpty()) {
                log.warn("Error updating role [ID: {}] - role not found", role.getId());
                return new UpdateRoleOutcome.NotFound(role.getId());
            }

            var roleEntity = roleOpt.get();

            if (roleEntity.isBuiltIn()) {
                log.warn("Error updating role {} - built-in roles cannot be updated", role);
                return new UpdateRoleOutcome.BuiltInRole(role);
            }

            if (!Objects.equals(roleEntity.getCode(), role.getCode())) {
                log.warn("Error updating role {} - invalid operation: cannot update role's code", role);
                return new UpdateRoleOutcome.InvalidChangeNameAttempt(role.getId(), role.getCode());
            }

            roleMapper.updateRole(roleEntity, role);

            RoleAdapter.UpdateKeycloakRoleOutCome updateOutcome = roleAdapter.updateRole(role);

            return switch (updateOutcome) {
                case RoleAdapter.UpdateKeycloakRoleOutCome.Success success -> {
                    log.info(
                            "updateRole: role [{}] on Keycloak",
                            success.roleUpdated().getCode());
                    yield new UpdateRoleOutcome.Success(roleMapper.toRole(roleEntity, getEnabledAttrs(role.getCode())));
                }

                case RoleAdapter.UpdateKeycloakRoleOutCome.NotFound notfound -> {
                    log.info("updateRole: role [{}] not found on keycloak", role.getCode());
                    yield new UpdateRoleOutcome.NotFound(role.getId());
                }

                case RoleAdapter.UpdateKeycloakRoleOutCome.InvalidInput nv -> {
                    log.warn("Error updating role [{}] - invalid operation: cannot update role's name", role);
                    yield new UpdateRoleOutcome.InvalidChangeNameAttempt(role.getId(), role.getCode());
                }
            };

        } catch (WebApplicationException e) {
            log.error("error updating role [{}] to realm", role.getCode());
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    @Transactional
    public DeleteRoleOutcome delete(@NotNull UUID roleId) {

        log.info("Deleting role with id {}", roleId);

        var roleEntityOpt = roleRepository.findById(roleId);

        if (roleEntityOpt.isEmpty()) {
            log.warn("Role with id {} not found.", roleId);
            return new DeleteRoleOutcome.NotFound(roleId);
        }

        if (roleEntityOpt.get().isBuiltIn()) {
            log.warn("Error deleting role with [ID: {}] - built-in roles cannot be updated", roleId);
            return new DeleteRoleOutcome.BuiltInRole(roleId);
        }

        var isAssigned = isRoleAssignedToAnyUser(roleEntityOpt.get().getCode());

        if (isAssigned) {
            log.info("Role with name [{}] is already assigned to at least one user", roleId);
            return new DeleteRoleOutcome.IsAssigned(roleId);
        }

        var roleCode = roleEntityOpt.get().getCode();

        roleRepository.delete(roleEntityOpt.get());

        var deleteKeycloakRoleOutcome = roleAdapter.deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteByName(roleCode));

        return switch (deleteKeycloakRoleOutcome) {
            case RoleAdapter.DeleteKeycloakRoleOutcome.Success success -> {
                log.info("Deleted: role with id [{}] on Keycloak successfully", roleId);
                identityAttributeRolesRepository.disableMappingByRoleCode(
                        success.role().getCode());
                yield new DeleteRoleOutcome.Success(roleId);
            }
            case RoleAdapter.DeleteKeycloakRoleOutcome.NotFound notfound -> {
                log.error("Inconsistent state: role with id {} not found when checking assignment", roleId);
                throw new IllegalStateException(String.format(
                        "Inconsistent state: role with id %s not found when checking assignment", roleId));
            }
        };
    }

    private boolean isRoleAssignedToAnyUser(String roleCode) {

        var assignmentOutcome = roleAdapter.isRoleAssigned(roleCode);

        switch (assignmentOutcome) {
            case RoleAdapter.IsKeycloakRoleAssignedOutcome.NotFound nf -> {
                log.error("Inconsistent state: role with name {} not found when checking assignment", roleCode);
                throw new IllegalStateException(String.format(
                        "Inconsistent state: role with name %s not found when checking assignment", roleCode));
            }
            case RoleAdapter.IsKeycloakRoleAssignedOutcome.Success outcome -> {
                return outcome.assigned();
            }
        }
    }

    @Override
    public SearchRolesOutcome search(@Valid @NotNull RoleFilter filter, Pageable pageable) {

        log.info("Searching roles with filters: [{}] - [PAGEABLE: {}]", filter, pageable);

        var spec = new RoleSpecification(filter);

        var page = roleRepository.findAll(spec, pageable);

        log.info("Searching roles with filters: [{}] - [PAGE-FOUND: {}]", filter, page);

        return new SearchRolesOutcome.Success(roleMapper.toRolePage(page));
    }

    private List<IdentityAttributeRole> getEnabledAttrs(String roleCode) {
        log.info("Getting enabled attributes of role {}", roleCode);
        return identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(roleCode);
    }

    private void rollbackImportedRoles(List<Role> importedRoles) {
        importedRoles.forEach(r -> roleAdapter.deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteByName(r.getCode())));
    }

    private CreateRoleOutcome doCreate(Role role) {

        log.info("creating role {}", role.getCode());

        if (role.isBuiltIn()) {
            log.warn("Error creating role {} - built-in roles cannot be created", role);
            return new CreateRoleOutcome.BuiltInRole(role);
        }

        if (roleRepository.findByCode(role.getCode()).isPresent()) {
            log.warn("Error Creating Role with name [NAME: {}] - role already exists", role.getCode());
            return new CreateRoleOutcome.Duplicated(role);
        }

        var createKeycloakRoleOutcome = roleAdapter.createRole(role);
        return switch (createKeycloakRoleOutcome) {
            case RoleAdapter.CreateKeycloakRoleOutcome.Success success -> {
                log.info("Successfully created role {}", success.role().getCode());
                role.setId(success.role().getId());
                var createdRoleEntity = roleRepository.saveAndFlush(roleMapper.toRoleEntity(role));
                var createdRole = roleMapper.toRole(createdRoleEntity);
                log.info("Created role entity: [ROLE-ENTITY: {}] - [ROLE-MODEL: {}]", createdRoleEntity, createdRole);
                yield new CreateRoleOutcome.Success(createdRole);
            }
            case RoleAdapter.CreateKeycloakRoleOutcome.Duplicated duplicated -> {
                log.warn("Error creating role - Duplicated role {}", role.getCode());
                yield new CreateRoleOutcome.Duplicated(role);
            }
        };
    }
}
