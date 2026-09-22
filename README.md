# Users And Roles

> ⚠️ **Modified work — CNIE-ES fork.**
> This repository is **not** the original SIMPL users-roles. It is a derivative work based on the
> upstream `2.11.0` line (commit `a0badf5c`,
> [upstream](https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles)),
> modified by the EDNEL-RIOJA project team for CNIE-ES between **2026-03-03 and 2026-09-18** to add
> the participant identifier and organization attributes to user onboarding. Distributed as
> `2.11.1-edval` under the **EUPL-1.2**, the same licence as the original work. Full details of what
> was changed and when: [NOTICE.EDNEL.md](NOTICE.EDNEL.md).

> **Purpose**: IAA Users And Roles is a microservice responsible for managing Users and Roles within the IAA ecosystem.
---

## 📑 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [⚡ Quick Start](#-quick-start)
    - [Run Locally](#run-locally)
4. [Installation guide](#installation-guide)
5. [User Guide](#user-guide)
6. [Testing](#testing)
7. [Contact & Support](#contact--support)
8. [License](#-license)

---

## Overview

The IAA Users And Roles microservice is a critical component within the Simpl ecosystem. Its main responsibilities include:

- Creating new roles and users

- Creating role requests for requesting a new role

- Updating existing roles, users and role requests

- Searching, filtering, and paginating roles, users and role requests

- Managing users and their associated roles

- Managing assignable identity attributes associated with roles

---

## Prerequisites

The project is tested against the following toolchain versions. Using different major versions may lead to build issues.

| Tool       | Required / Tested Versions | Notes                                                        |
|------------|----------------------------|--------------------------------------------------------------|
| Java       | 21                         | ------                                                       |
| Maven      | 3.9+                       | Package manager for java dependencies.                       |
| Git        | 2.30.0+                    | Source control.                                              |
| Docker     | Latest stable              | For container builds via provided `Dockerfile`.              |
| Kubernetes | Latest stable              | For managing cluster and scalability/availability of cluster |
| Helm       | 3.19.0+                    | For Kubernetes reusable charts                               |


## ⚡ Quick Start

### Run Locally

Steps to quickly run the service locally for development or testing:

```bash
# Navigate to your project directory
cd <path>
# Clone the project from repository
git clone https://code.europa.eu/simpl/simpl-open/development/iaa/users-roles.git
# Build the project
mvn clean package
# Run the service
java -jar usersroles-<version>.jar or generate a runner inside your IDE
# API documentation can be found in openapi folder
curl -X <http-operation> https://localhost:8080/{api-version}/<api>
```

---

## Installation guide

For the user guide, please refer to the navigation below:

- [`deployment-guide`](documents/deployment-guide)
- [`installation-guide`](documents/installation-guide)
- [`upgrade-guide`](documents/upgrade-guide)

---

## User guide

For the user guide, please refer to:

- [`user-manual`](documents/user-manual)

---

## Testing

Steps to execute the test suite:

```bash
mvn test
```


---

## Contact & Support

- **Issue Tracker**: Please submit your issues and feature requests via the GitLab issue tracker of the repository.
- **Support**: cnect-simpl@ec.europa.eu

---

📌 _This README is part of the **Simpl-Open** project documentation standards. Every component
repository should maintain an up-to-date README covering the sections above._


## Licence

The original work, users-roles, is © European Union / SIMPL Programme and is licensed under the
**European Union Public Licence v. 1.2 (EUPL-1.2)**, whose full official text is reproduced in
[LICENSE](LICENSE). Third-party components included in the product are listed in [NOTICE](NOTICE) /
[NOTICE.json](NOTICE.json) / [THIRD_PARTY_LICENCES.md](THIRD_PARTY_LICENCES.md), and credited in
[CREDITS.pdf](CREDITS.pdf).

This fork is a **modified version** of that work, distributed under the same licence. The
modification notice required by Art. 5 of the EUPL (what was modified, by whom and when) is in
[NOTICE.EDNEL.md](NOTICE.EDNEL.md). The complete corresponding source code, including the revision
history, is available at https://github.com/cnie-es/simpl-users-roles.

---
