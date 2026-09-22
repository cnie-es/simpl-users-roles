package eu.europa.ec.simpl.usersroles.services.impl;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

import eu.europa.ec.simpl.usersroles.adapters.IdentityAttributeAdapter;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.services.IdentityAttributeService;
import eu.europa.ec.simpl.usersroles.services.model.InvalidOutput;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Log4j2
@Service
@Validated
@RequiredArgsConstructor
public class IdentityAttributeServiceImpl implements IdentityAttributeService {

    private final IdentityAttributeRolesRepository idaRolesRepository;
    private final IdentityAttributeAdapter identityAttributeAdapter;

    @Override
    @Transactional
    public void assignIdentityAttributes(@NotNull List<@NotBlank String> identityAttributeCodes) {
        log.info("Start assign identity attributes {}", identityAttributeCodes);
        idaRolesRepository.updateAssignments(identityAttributeCodes);
        log.info("End assign identity attributes {}", identityAttributeCodes);
    }

    @Override
    public ValidateIdentityAttributeOutcome validateIdentityAttributesAssignableToRoles(
            @NotNull List<@NotBlank String> identityAttributesCode) {
        log.info("Start validation identity attributes assignable to roles {}", identityAttributesCode);
        var errors = new ArrayList<InvalidOutput>();
        for (var code : identityAttributesCode) {
            var oIda = identityAttributeAdapter.findIdentityAttribute(code);

            if (oIda.isEmpty()) {
                log.warn("Identity attribute {} was not found in the authentication provider.", code);
                errors.add(new InvalidOutput.NotFound(code));
            } else {
                var ida = oIda.get();
                log.debug("Found identity attribute {}", code);

                if (isFalse(ida.getEnabled())) {
                    log.warn("Identity attribute {} is not enabled.", code);
                    errors.add(new InvalidOutput.NotEnabled(code));
                }

                if (isFalse(ida.getAssignableToRoles())) {
                    log.warn("Identity attribute {} is not assignable to roles.", code);
                    errors.add(new InvalidOutput.NotAssignableToRole(code));
                }

                if (isFalse(ida.getAssignedToParticipant())) {
                    log.warn("Identity attribute {} is not assigned to participant.", code);
                    errors.add(new InvalidOutput.NotAssignedToParticipant(code));
                }
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Validation failed, found {} errors for identity attributes", errors.size());
            log.debug("Validation failed, errors -> {}", errors);
            return new ValidateIdentityAttributeOutcome.Invalid(errors);
        }

        log.info("Identity attributes validated successfully: {}", identityAttributesCode);
        return new ValidateIdentityAttributeOutcome.Success();
    }
}
