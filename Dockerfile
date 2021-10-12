FROM navikt/java:11-appdynamics

COPY build/libs/eessi-pensjon-begrens-innsyn-0.0.1.jar /app/app.jar

COPY nais/export-vault-secrets.sh /init-scripts/

ENV APPD_ENABLED true
ENV APPD_NAME eessi-pensjon
ENV APPD_TIER begrens-innsyn
