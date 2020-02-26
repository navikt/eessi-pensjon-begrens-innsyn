FROM navikt/java:8-appdynamics

COPY build/libs/eessi-pensjon-begrens-innsyn-*.jar /app/app.jar

COPY nais/export-vault-secrets.sh /init-scripts/
RUN chmod +x /init-scripts/*
