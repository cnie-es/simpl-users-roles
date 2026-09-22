# Upgrade guide

## Upgrade to v2.11.x

No relevant changes are made on charts in this version, so it is enough to upgrade the version via helm upgrade:

```bash
helm upgrade users-roles users-roles-charts/users-roles \
--version 2.11.0 \
--values values.yaml
```

## Upgrade to v2.10.x

No relevant changes are made on charts in this version, so it is enough to upgrade the version via helm upgrade:

```bash
helm upgrade users-roles users-roles-charts/users-roles \
--version 2.10.0 \
--values values.yaml
```

## Upgrade to v2.9.x

No relevant changes are made on charts in this version, so it is enough to upgrade the version via helm upgrade:

```bash
helm upgrade users-roles users-roles-charts/users-roles \
--version 2.9.0 \
--values values.yaml
```

## Upgrade up to version 2.7
For upgrades prior to version 2.7, please refer to the 'UPGRADING.md' file in the appropriate version of the 'version_docs' (https://code.europa.eu/simpl/simpl-open/development/iaa/documentation/-/tree/main/versioned_docs) folder within the central documentation repository.