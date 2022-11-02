FROM ghcr.io/navikt/baseimages/temurin:17

COPY init-scripts/ep-jvm-tuning.sh /init-scripts/

COPY build/libs/eessi-pensjon-begrens-innsyn-0.0.1.jar /app/app.jar
