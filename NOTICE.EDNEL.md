# Modification notice (EUPL 1.2, Art. 5)

**This is a modified version of SIMPL users-roles. It is not the original work.**

The original work, users-roles, is part of the SIMPL programme (© European Union / SIMPL
Programme) and is licensed under the **European Union Public Licence v. 1.2 (EUPL-1.2)**. See
[LICENSE](LICENSE), where the full official text of the licence is reproduced, and [NOTICE](NOTICE)
/ [NOTICE.json](NOTICE.json) / [THIRD_PARTY_LICENCES.md](THIRD_PARTY_LICENCES.md) /
[CREDITS.pdf](CREDITS.pdf) for the third-party components included in it. All original copyright,
licence and disclaimer notices are kept intact and unmodified in this fork.

## Upstream baseline

| | |
|---|---|
| Original work | users-roles (`eu.europa.ec.simpl:usersroles`) |
| Upstream repository | https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles |
| Baseline version | `2.11.0` |
| Baseline commit | `a0badf5c7403cc4c0095967f1aa6fc7f92b27450` (2026-02-18) |

## Modifications

| | |
|---|---|
| Modified by | EDNEL-RIOJA project team, for CNIE-ES |
| Public repository of this derivative work | https://github.com/cnie-es/simpl-users-roles |
| Version of this derivative work | `2.11.1-edval` |
| Dates of modification | **2026-03-03 to 2026-09-18** |

The modifications are licensed under the **EUPL-1.2**, the same licence as the original work.

The complete source code of this derivative work is available at the public repository above as a
vetted release snapshot, and will remain freely available there for as long as the Work is
distributed. The upstream repository and exact baseline commit are recorded above. Each release
snapshot includes `SBOM.cyclonedx.json`, which binds the upstream and work revisions, release tag,
public repository, snapshot hash and image digest. The distribution history contains release
snapshots rather than a copy of the upstream Git history; the comparison instructions below fetch
the baseline from its authoritative upstream repository. The tables below record what each file
contributed. The published container images (`ghcr.io/cnie-es/users-roles`) are built from that
snapshot.

### Summary of the changes

- **Participant identifier and organization attributes** on users: the participant identifier can
  be set on a user, the organization attribute is recorded when one is created, and both are
  returned by the API. The change runs through `UserAdapterImpl`, `KeycloakMapper`,
  `UserMapperTier1V2`, the `User` model and the OpenAPI description.
- The password is optional when onboarding a new user.
- `pom.xml`: fork-specific coordinates, version, source URL and developer metadata were added. The
  upstream parent and dependency repository declarations remain unchanged.

A second, non-functional group of changes (2026-09-11) adds the notices this licence requires of a
derivative work: this file, the notice at the top of [README.md](README.md), the licence and
source-code metadata in `pom.xml` and in the OCI labels of the `Dockerfile`, and the reproduction of
the full official licence text inside [LICENSE](LICENSE), which previously only linked to it. See
[CHANGELOG.md](CHANGELOG.md) for the itemised list.

A further non-functional publication change (2026-09-18) pins the container base image by digest,
updates the Helm chart to identify the component-specific GHCR package, and aligns the public
README, Maven metadata and changelog with the final modification period.

### Files added

| File | Date |
|---|---|
| `NOTICE.EDNEL.md` (this file) | 2026-09-11, 2026-09-14, 2026-09-18 |

### Files modified

| File | Date |
|---|---|
| `CHANGELOG.md` | 2026-09-11, 2026-09-18 |
| `Dockerfile` | 2026-09-11, 2026-09-18 |
| `LICENSE` | 2026-09-11 |
| `README.md` | 2026-09-11, 2026-09-18 |
| `charts/Chart.yaml` | 2026-09-11 |
| `charts/values.yaml` | 2026-09-11, 2026-09-18 |
| `openapi/applicationinfo-v1.yaml` | 2026-09-11 |
| `openapi/usersroles-tier1-v2.yaml` | 2026-09-11 |
| `openapi/usersroles-v1.yaml` | 2026-04-29, 2026-09-14 |
| `pipeline.variables.sh` | 2026-09-11 |
| `pom.xml` | 2026-04-29, 2026-09-11, 2026-09-14, 2026-09-18 |
| `src/main/java/eu/europa/ec/simpl/usersroles/adapters/impl/UserAdapterImpl.java` | 2026-04-29 |
| `src/main/java/eu/europa/ec/simpl/usersroles/adapters/mappers/KeycloakMapper.java` | 2026-04-29,2026-05-05 |
| `src/main/java/eu/europa/ec/simpl/usersroles/controllers/mappers/UserMapperTier1V2.java` | 2026-04-29 |
| `src/main/java/eu/europa/ec/simpl/usersroles/controllers/mappers/UserMapperV1.java` | 2026-09-14 |
| `src/main/java/eu/europa/ec/simpl/usersroles/models/User.java` | 2026-04-29 |

No file of the original work has been removed, and no copyright, licence or disclaimer notice of
the original work has been altered. The only change to [LICENSE](LICENSE) is the addition, below
the original heading and credits line, of the full official text of the EUPL-1.2 that the file
previously referenced only by hyperlink; nothing in the original file was removed or reworded.

The per-file diff is reproducible by fetching the authoritative upstream history into a clone of
the public release repository and comparing the recorded baseline with the release tag:

```
git remote add upstream https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles.git
git fetch upstream
git diff a0badf5c7403cc4c0095967f1aa6fc7f92b27450..<release-tag>
```
