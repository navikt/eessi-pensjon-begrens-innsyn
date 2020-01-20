#!/usr/bin/env bash

if test -f /var/run/secrets/nais.io/srveessi-pensjon-j/password;
then
    echo "Setting SYSTEMBRUKER_PASSWORD"
    export  SYSTEMBRUKER_PASSWORD=$(cat /var/run/secrets/nais.io/srveessi-pensjon-j/password)
fi

if test -f /var/run/secrets/nais.io/srveessi-pensjon-j/username;
then
    echo "Setting SYSTEMBRUKER_USERNAME"
    export  SYSTEMBRUKER_USERNAME=$(cat /var/run/secrets/nais.io/srveessi-pensjon-j/username)
fi