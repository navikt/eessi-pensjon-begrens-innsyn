FROM navikt/java:17-appdynamics

COPY build/libs/eessi-pensjon-begrens-innsyn-0.0.1.jar /app/app.jar

ENV APPD_ENABLED true
ENV APPD_NAME eessi-pensjon
ENV APPD_TIER begrens-innsyn
