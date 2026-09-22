## 2.11.1-edval (2026-09-18)

> Derivative work by the **EDNEL-RIOJA** project team for **CNIE-ES**, based on the upstream
> users-roles `2.11.0` line (commit `a0badf5c`). Modified between **2026-03-03 and 2026-09-18**,
> licensed under EUPL-1.2 like the original work. See [NOTICE.EDNEL.md](NOTICE.EDNEL.md) for the
> full modification notice.

### Added (2026-03-03 → 2026-07-10)

- **Participant identifier and organization attributes** on users: the participant identifier can
  now be set on a user, the organization attribute is recorded when creating one, and both are
  returned by the API. Carried through `UserAdapterImpl`, `KeycloakMapper`, `UserMapperTier1V2`,
  the `User` model and the OpenAPI description.
- GitHub Actions pipelines: one that builds and publishes the container image, on pushes and on
  pull requests targeting `ednel`, and one that syncs this fork from the upstream repository.

### Changed (2026-04-29 → 2026-05-05)

- The password is optional when onboarding a new user.

### Licence compliance (2026-09-11)

Notices required by Art. 5 of the EUPL-1.2 (Attribution right, Provision of Source Code) for this
derivative work:

- `NOTICE.EDNEL.md`: modification notice stating that the work has been modified, by whom, when and
  what was changed, with the repository where the complete corresponding source code is available.
- `README.md`: prominent notice at the top of the file identifying this repository as a modified
  version of users-roles, plus a Licence section.
- `LICENSE`: the full official text of the EUPL-1.2 is now reproduced in the file, which previously
  only linked to it, so that a copy of the Licence travels with every copy of the Work. The
  original SIMPL heading and credits line are kept intact.
- `Dockerfile`: OCI image labels (`licenses`, `source`, `vendor`, `description`) and `LICENSE`,
  `NOTICE`, `NOTICE.json`, `CREDITS.pdf`, `THIRD_PARTY_LICENCES.md` and `NOTICE.EDNEL.md` copied
  into `/licenses/`, so the notices and the pointer to the source code travel with the published
  container image.
- `pom.xml`: the project now declares its own `groupId`. Without one it inherited
  `eu.europa.ec.simpl` from the parent, which identified a modified artifact under a namespace
  CNIE-ES does not control (Art. 5, Legal Protection); the parent coordinate is left untouched,
  since there it correctly identifies a third-party artifact. The artifact is renamed to
  `simpl-users-roles`, and `scm`, `url` and `developers` metadata filled in. The existing
  `licenses` declaration is kept.
  The **Maven project version is `2.11.1-edval`** and matches the version declared in
  `pipeline.variables.sh`, the chart `version` and `appVersion`, the OCI `version` label and the
  published image tag. The upstream parent coordinate remains at `2.11.0`, where it correctly
  identifies the third-party build parent.
- `.github/workflows/build-and-push-image.yaml`: the container image namespace is derived from the
  repository owner instead of being hard-coded, and the release version — read from
  `pipeline.variables.sh`, not from the POM — is published as an image tag alongside the commit SHA,
  so the tag the chart resolves to by default exists. The build fails if the release version and
  the chart `appVersion` ever drift apart.
- Helm chart made loadable outside the upstream GitLab pipeline: `Chart.yaml` and `values.yaml`
  carried unsubstituted `${PROJECT_RELEASE_VERSION}` and `${CI_REGISTRY_IMAGE}` placeholders, which
  that pipeline replaced and which made the chart fail to load anywhere else. `image.repository`
  now defaults to the published image of this fork. The chart already handled `imagePullSecrets`
  and the `appVersion` tag fallback, so those were left as they were.
- `pipeline.variables.sh`: `PROJECT_VERSION_NUMBER` now carries the release version,
  `2.11.1-edval`. It declared `2.11.1`, which named no artefact that was ever published.

### Publication hardening (2026-09-18)

- The `Dockerfile` base image is pinned by digest so the release build uses an immutable Java 21
  base.
- The Helm chart references the component-specific package `ghcr.io/cnie-es/users-roles` instead
  of the legacy aggregate package path.
- Public provenance metadata and the per-file modification notice are aligned with these
  non-functional publication changes.


## 2.4.0 (2025-09-08)

### fixed (1 change)

- [[SIMPL-16771](https://jira.simplprogramme.eu/browse/SIMPL-16771) PSO | Error...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/6895e4c5f73c2bcba37ecdbbdf5e12d7f052c07f) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/265))

### added (1 change)

- [[SIMPL-14971](https://jira.simplprogramme.eu/browse/SIMPL-14971) PSO | Missing...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/2cc54963d7405b8c5d39c8b1af72d284df051b67) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/262))



## 2.4.0 (2025-09-08)

### fixed (1 change)

- [[SIMPL-16771](https://jira.simplprogramme.eu/browse/SIMPL-16771) PSO | Error...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/6895e4c5f73c2bcba37ecdbbdf5e12d7f052c07f) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/265))

### added (1 change)

- [[SIMPL-14971](https://jira.simplprogramme.eu/browse/SIMPL-14971) PSO | Missing...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/2cc54963d7405b8c5d39c8b1af72d284df051b67) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/262))



## 2.3.0 (2025-08-06)

### fixed (2 changes)

- [[SIMPL-14719](https://jira.simplprogramme.eu/browse/SIMPL-14719) Added...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/40e78bf5dc8bad1489042b5cb51a15aadc61a0d5) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/257))
- [[SIMPL-9787](https://jira.simplprogramme.eu/browse/SIMPL-9787) PSO | [API]...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/7b02058eafcbbc1f8abcfb5621b6ae8de24979f5) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/255))


## 2.2.0 (2025-07-14)

### added (2 changes)

- [[SIMPL-13047](https://jira.simplprogramme.eu/browse/SIMPL-13047) Integrate...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/d379a26f021d9bc4fe7f1883f59b62498b47a535) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/251))
- [Add Spring HATEOAS dependency to support generation of paged resource links...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/a3b73898a79f0d294e09217be5dbaa0b792270a5) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/252))


## 2.1.0 (2025-06-20)

### changed (2 changes)

- [Improve identity attributes synchronization](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/69e36b455c426793f753e10fed14a132920a5f86) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/243))
- [Improve identity attributes synchronization](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/43ce035cdce3211645d60103026d26ce50babbcb) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/243))

### other (1 change)

- [[SIMPL-13669](https://jira.simplprogramme.eu/browse/SIMPL-13669) Analysis and...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/4968753cd2e1716264ed210e465078ef6e4a0c25) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/237))


## 2.0.1 (2025-06-20)

### fixed (1 change)

- [Fixed openapi's version](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/77be84f7ffb028e0c5c6a8067a270bee431f6447) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/247))


## 2.0.0 (2025-06-03)

### other (3 changes)

- [Drop table identity attribute](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/069bb3ca77bbc3aeee2c1e0f5c58fed4247dc4f1) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/232))
- [README.md - Added Configuration Properties section](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/ffd5da3e38f465727fc2548da0c2894b9ced977d) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/231))
- [README.md - Added API Documentation section](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/09416502ce441fffd38c6fc23228d7cb40ec413f) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/231))

### added (2 changes)

- [Maven goal to automatically add openapi from simpl-api-iaa](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/01abae0746ae483ece509e9ff7ef9c605dacf57f) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/224))
- [Search roles by multiple names](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/8436b86760d63352aceaa1ea3555c7e35d054081) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/225))

### removed (4 changes)

- [Removed deprecated API](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/5dfa0bdc2e848bbdaf85436ed4019c4ed1ff2a9d) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/226))
- [Removed ingress spec from chart](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/955b44ce6b833526c93474865cb599c8cf520ab8) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/223))
- [Removed keycloak.client-to-realm-role-migration properties](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/fc1421fd95ce5d9aba88b37f79975e7c63c44817) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/223))
- [[SIMPL-11765](https://jira.simplprogramme.eu/browse/SIMPL-11765) Remove...](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/2ab95958d6c3a5a98ccff9cf3b85674a9d16c53f) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/223))


## 1.5.0 (2025-05-12)

### added (1 change)

- [[SIMPL-12213](https://jira.simplprogramme.eu/browse/SIMPL-12213) Licenses files in the repositories](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/c454e515d15a14ab153db8b7d5d422b9e40e641d) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/219))


## 1.4.0 (2025-04-22)

No changes.


## 1.3.1 (2025-04-11)

No changes.


## 1.3.0 (2025-03-31)

### changed (1 change)

- [Replaced StreamingResponseBody with Resource](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/d8daa02868eac25b3f7f332d0ad61a371703bd4a) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/206))

### fixed (1 change)

- [SIMPL-10198 Bugfix](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/700c7ab90cff457f6b62e4b9238f4a75d9b362a7) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/200))

### added (1 change)

- [Event management for deleted onboarding requests](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/commit/213d2931bfcefab4fdad79717ce68b6902b32877) ([merge request](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles/-/merge_requests/202))
