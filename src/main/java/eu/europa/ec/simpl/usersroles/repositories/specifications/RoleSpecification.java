package eu.europa.ec.simpl.usersroles.repositories.specifications;

import eu.europa.ec.simpl.usersroles.entities.IdentityAttributeRole_;
import eu.europa.ec.simpl.usersroles.entities.RoleEntity;
import eu.europa.ec.simpl.usersroles.entities.RoleEntity_;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
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
public class RoleSpecification implements Specification<RoleEntity> {

    private static final long serialVersionUID = 1L;

    private final transient RoleFilter filter;

    public RoleSpecification(RoleFilter filter) {
        this.filter = filter;
    }

    public Specification<RoleEntity> hasId() {
        return (root, query, cb) -> filter.getId() == null ? null : cb.equal(root.get(RoleEntity_.id), filter.getId());
    }

    public Specification<RoleEntity> nameIn() {

        return (root, query, cb) -> {
            if (filter.getName() == null || filter.getName().isEmpty()) {
                return null;
            }

            var predicates = filter.getName().stream()
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .map(s -> s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))
                    .map(escaped -> cb.like(cb.lower(root.get(RoleEntity_.name)), "%" + escaped + "%", '\\'))
                    .toArray(Predicate[]::new);

            return predicates.length == 0 ? cb.conjunction() : cb.or(predicates);
        };
    }

    public Specification<RoleEntity> hasCode() {
        return (root, query, cb) -> {
            if (StringUtils.isBlank(filter.getCode())) {
                return null;
            }

            String raw = filter.getCode().trim().toLowerCase();

            String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

            return cb.like(cb.lower(root.get(RoleEntity_.code)), "%" + escaped + "%", '\\');
        };
    }

    public Specification<RoleEntity> hasDescription() {
        return (root, query, cb) -> {
            if (StringUtils.isBlank(filter.getDescription())) {
                return null;
            }

            String raw = filter.getDescription().trim().toLowerCase();

            String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

            return cb.like(cb.lower(root.get(RoleEntity_.description)), "%" + escaped + "%", '\\');
        };
    }

    public Specification<RoleEntity> hasEnabled() {
        return (root, query, cb) ->
                filter.getEnabled() == null ? null : cb.equal(root.get(RoleEntity_.enabled), filter.getEnabled());
    }

    public Specification<RoleEntity> hasBuiltIn() {
        return (root, query, cb) ->
                filter.getBuiltIn() == null ? null : cb.equal(root.get(RoleEntity_.builtIn), filter.getBuiltIn());
    }

    public Specification<RoleEntity> hasAttributeName() {
        return (root, query, cb) -> {
            if (StringUtils.isBlank(filter.getAttributeName())) {
                return null;
            }

            var identityAttributeRolesJoin = root.join(RoleEntity_.identityAttributeRoles);

            return cb.and(
                    cb.isTrue(identityAttributeRolesJoin.get(IdentityAttributeRole_.enabled)),
                    cb.equal(
                            identityAttributeRolesJoin.get(IdentityAttributeRole_.idaCode), filter.getAttributeName()));
        };
    }

    @Override
    public Predicate toPredicate(Root<RoleEntity> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        if (query.getResultType() != Long.class) {
            root.fetch(RoleEntity_.identityAttributeRoles, JoinType.LEFT);
        }

        return Specification.allOf(
                        hasId(), nameIn(), hasCode(), hasDescription(), hasEnabled(), hasBuiltIn(), hasAttributeName())
                .toPredicate(root, query, criteriaBuilder);
    }
}
