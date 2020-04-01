![](https://github.com/navikt/eessi-pensjon-begrens-innsyn/workflows/Bygg%20og%20deploy%20Q2/badge.svg)
![](https://github.com/navikt/eessi-pensjon-begrens-innsyn/workflows/Manuell%20deploy/badge.svg)

# eessi-pensjon-begrens-innsyn

## Komponentstruktur

![](./components.svg)
<img src="./components.svg">

# Utvikling

## Komme i gang

```
./gradlew build
```

## Oppdatere avhengigheter

Sjekke om man har utdaterte avhengigheter (forsøker å unngå milestones og beta-versjoner):

```
./gradlew dependencyUpdates
```

Dersom du er supertrygg på testene kan du forsøke en oppdatering av alle avhengighetene:


```
./gradlew useLatestVersions && ./gradlew useLatestVersionsCheck
```
