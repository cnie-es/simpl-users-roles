FROM eclipse-temurin:21-jdk-alpine@sha256:cd87715a8d45cfaa42419207c64680234f62785c49055cccd20437b5c9018380

ARG IMAGE_REVISION="0000000000000000000000000000000000000000"
ARG IMAGE_CREATED="1970-01-01T00:00:00Z"

# EUPL-1.2 (Art. 5): this image ships a modified version of SIMPL users-roles. The licence, the
# third-party notices and the modification notice travel with the image, and the labels below point
# to the repository where the complete corresponding source code is available.
LABEL org.opencontainers.image.title="users-roles (CNIE-ES fork)" \
      org.opencontainers.image.description="Modified version of SIMPL users-roles (upstream commit a0badf5c), modified by the EDNEL-RIOJA project team for CNIE-ES between 2026-03-03 and 2026-09-18. See /licenses/NOTICE.EDNEL.md." \
      org.opencontainers.image.version="2.11.2-edval" \
      org.opencontainers.image.vendor="CNIE-ES" \
      org.opencontainers.image.licenses="EUPL-1.2" \
      org.opencontainers.image.source="https://github.com/cnie-es/simpl-users-roles" \
      org.opencontainers.image.revision="${IMAGE_REVISION}" \
      org.opencontainers.image.created="${IMAGE_CREATED}"

RUN adduser -S -u 1001 1001

COPY target/*.jar app.jar

# The notices must travel with every copy of the Work (EUPL-1.2, Art. 5).
COPY LICENSE NOTICE NOTICE.json CREDITS.pdf THIRD_PARTY_LICENCES.md NOTICE.EDNEL.md /licenses/

RUN chown 1001 /app.jar

USER 1001

ENTRYPOINT ["java","-jar","/app.jar"]
