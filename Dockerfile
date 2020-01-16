FROM navikt/java:8

RUN make

COPY build/libs/eessi-pensjon-begrens-innsyn-*.jar /app/app.jar
