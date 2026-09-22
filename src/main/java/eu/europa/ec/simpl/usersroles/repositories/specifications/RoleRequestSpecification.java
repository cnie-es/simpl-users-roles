package eu.europa.ec.simpl.usersroles.repositories.specifications;

import eu.europa.ec.simpl.usersroles.entities.RoleRequestEntity;
import eu.europa.ec.simpl.usersroles.entities.RoleRequestEntity_;
import eu.europa.ec.simpl.usersroles.models.RoleRequestFilter;
import eu.europa.ec.simpl.usersroles.models.RoleRequestStatusEnum;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

@EqualsAndHashCode()
@Getter
public class RoleRequestSpecification implements Specification<RoleRequestEntity> {

    private static final long serialVersionUID = 1L;

    private final transient RoleRequestFilter filter;

    public RoleRequestSpecification(RoleRequestFilter filter) {
        this.filter = filter;
    }

    public Specification<RoleRequestEntity> hasEmail() {
        return (root, query, cb) -> {
            if (StringUtils.isBlank(filter.getEmail())) {
                return null;
            }

            String raw = filter.getEmail().trim().toLowerCase();

            String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

            return cb.like(cb.lower(root.get(RoleRequestEntity_.userEmail)), "%" + escaped + "%", '\\');
        };
    }

    public Specification<RoleRequestEntity> hasStatus() {
        return (root, query, cb) -> {
            if (StringUtils.isBlank(filter.getStatus())) {
                return null;
            }
            try {
                RoleRequestStatusEnum status =
                        RoleRequestStatusEnum.valueOf(filter.getStatus().toUpperCase());
                return cb.equal(root.get(RoleRequestEntity_.status), status);
            } catch (IllegalArgumentException ex) {
                return cb.disjunction();
            }
        };
    }

    @Override
    public Predicate toPredicate(
            Root<RoleRequestEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        if (query.getResultType() != Long.class) {
            root.fetch(RoleRequestEntity_.rolesRequested, JoinType.LEFT);
            query.distinct(true);
        }

        return Specification.allOf(hasEmail(), hasStatus()).toPredicate(root, query, criteriaBuilder);
    }
}
