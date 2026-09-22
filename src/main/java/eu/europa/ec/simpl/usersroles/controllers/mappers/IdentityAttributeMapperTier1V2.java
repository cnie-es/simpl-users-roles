package eu.europa.ec.simpl.usersroles.controllers.mappers;

import eu.europa.ec.simpl.api.usersroles.t1.v2.model.IdentityAttributeDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, unmappedSourcePolicy = ReportingPolicy.ERROR)
public interface IdentityAttributeMapperTier1V2 {

    default String toCode(IdentityAttributeDTO dto) {
        return dto.getCode();
    }

    List<String> toCodes(List<IdentityAttributeDTO> dtos);

    IdentityAttributeDTO toDto(String code);
}
