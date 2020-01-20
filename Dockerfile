FROM navikt/java:11-appdynamics

COPY build/libs/eessi-pensjon-begrens-innsyn-*.jar /app/app.jar

COPY export-vault-secrets.sh /init-scripts/
RUN chmod +x /init-scripts/*

